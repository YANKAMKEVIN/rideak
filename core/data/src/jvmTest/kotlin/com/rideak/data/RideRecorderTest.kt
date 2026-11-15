package com.rideak.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rideak.data.db.RideakDatabase
import com.rideak.data.location.LocationSource
import com.rideak.data.recorder.GpsSignal
import com.rideak.data.recorder.RecorderState
import com.rideak.data.recorder.RideForegroundController
import com.rideak.data.recorder.RideRecorder
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RideRecorderTest {
    private val database = Room.inMemoryDatabaseBuilder<RideakDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomRideRepository(database.rideDao())
    private val fixes = MutableSharedFlow<TrackPoint>()
    private val foreground = object : RideForegroundController {
        var active = false
        override fun onRideActive() { active = true }
        override fun onRideEnded() { active = false }
    }
    private var ids = 0

    @AfterTest
    fun close() = database.close()

    private fun TestScope.recorder(scope: CoroutineScope = backgroundScope) = RideRecorder(
        repository = repository,
        locationSource = { LocationSource { fixes } },
        scope = scope,
        clock = { testScheduler.currentTime },
        newId = { "id-${ids++}" },
        foreground = foreground,
    )

    /** One fix per second heading east at 5 m/s, starting [fromMeters] east of Cergy. */
    private suspend fun TestScope.ride(seconds: Int, fromMeters: Double = 0.0) {
        repeat(seconds) { s ->
            val east = fromMeters + 5.0 * s
            fixes.emit(
                TrackPoint(
                    GeoPoint(49.0364, 2.0761 + east / (111_195.0 * cos(49.0364 * PI / 180))),
                    testScheduler.currentTime, horizontalAccuracyMeters = 5f, speedMetersPerSecond = 5f,
                ),
            )
            advanceTimeBy(1_000)
        }
    }

    private fun RideRecorder.active() = assertIs<RecorderState.Active>(state.value)

    @Test
    fun recordsARideAndPersistsItWhenStopped() = runTest {
        val recorder = recorder()
        val id = recorder.start()
        runCurrent()
        assertTrue(foreground.active)

        ride(seconds = 61) // 300 m
        assertTrue(recorder.active().metrics.distanceMeters in 290.0..310.0)
        assertEquals(GpsSignal.GOOD, recorder.active().signal)

        assertEquals(id, recorder.stop())
        assertEquals(RecorderState.Idle, recorder.state.value)
        assertTrue(!foreground.active)

        val saved = assertNotNull(repository.ride(id))
        assertEquals(RideStatus.FINISHED, saved.status)
        assertTrue(saved.distanceMeters in 290.0..310.0)
        assertEquals(61, repository.track(id).single().size)
        assertEquals(listOf(saved), repository.observeFinishedRides().first())
    }

    @Test
    fun pauseStopsTimeAndDistanceAndResumeOpensANewSegment() = runTest {
        val recorder = recorder()
        val id = recorder.start()
        runCurrent()
        ride(seconds = 21) // 100 m in 21 s
        recorder.pause()
        val pausedDuration = recorder.active().activeDurationMillis

        advanceTimeBy(300_000) // 5 min coffee break
        ride(seconds = 10, fromMeters = 2_000.0) // fixes while paused are ignored
        assertEquals(RideStatus.PAUSED, recorder.active().status)
        assertEquals(pausedDuration, recorder.active().activeDurationMillis)

        recorder.resume()
        runCurrent()
        ride(seconds = 21, fromMeters = 1_000.0) // jumped 900 m while paused: not counted
        recorder.stop()

        val saved = assertNotNull(repository.ride(id))
        assertTrue(saved.distanceMeters in 190.0..215.0, "distance ${saved.distanceMeters}")
        assertTrue(saved.activeDurationMillis in 41_000..43_000, "duration ${saved.activeDurationMillis}")
        assertEquals(2, repository.track(id).size)
    }

    @Test
    fun spotIsSavedAtTheCurrentPosition() = runTest {
        val recorder = recorder()
        assertNull(recorder.dropSpot()) // no ride
        val id = recorder.start()
        runCurrent()
        assertNull(recorder.dropSpot()) // no fix yet
        ride(seconds = 5)
        val spot = assertNotNull(recorder.dropSpot())
        assertEquals(1, recorder.active().spotCount)
        recorder.stop()
        assertEquals(listOf(spot), repository.spots(id))
        assertEquals(1, repository.ride(id)?.spotCount)
    }

    @Test
    fun anInterruptedRideIsRecoveredPausedWithItsMetrics() = runTest {
        // First "process": records then dies without stopping (its scope is cancelled).
        val firstProcess = CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.Job())
        val first = recorder(firstProcess)
        val id = first.start()
        runCurrent()
        ride(seconds = 41) // 200 m, flushed in batches
        firstProcess.coroutineContext[kotlinx.coroutines.Job]!!.cancel()

        // Second "process".
        val second = recorder()
        second.recoverUnfinished()
        val state = second.active()
        assertEquals(id, state.rideId)
        assertEquals(RideStatus.PAUSED, state.status)
        assertTrue(state.recovered)
        assertTrue(state.metrics.distanceMeters in 180.0..205.0, "recovered ${state.metrics.distanceMeters}")

        second.resume()
        runCurrent()
        ride(seconds = 21, fromMeters = 1_000.0)
        second.stop()
        assertTrue(assertNotNull(repository.ride(id)).distanceMeters in 280.0..310.0)
    }

    @Test
    fun signalIsSearchingWithoutRecentFixes() = runTest {
        val recorder = recorder()
        recorder.start()
        runCurrent()
        advanceTimeBy(2_000)
        recorder.awaitSignal(GpsSignal.SEARCHING)
        ride(seconds = 3)
        recorder.awaitSignal(GpsSignal.GOOD)
        advanceTimeBy(15_000)
        recorder.awaitSignal(GpsSignal.SEARCHING)
        recorder.stop()
    }

    @Test
    fun aFailingLocationStreamIsReportedAndRetried() = runTest {
        var attempts = 0
        val recorder = RideRecorder(
            repository = repository,
            locationSource = {
                LocationSource {
                    attempts++
                    if (attempts == 1) flow { throw IllegalStateException("permission revoked") } else fixes
                }
            },
            scope = backgroundScope,
            clock = { testScheduler.currentTime },
            newId = { "id-${ids++}" },
        )
        recorder.start()
        runCurrent()
        recorder.awaitSignal(GpsSignal.UNAVAILABLE)

        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(2, attempts)
        ride(seconds = 3)
        recorder.awaitSignal(GpsSignal.GOOD)
        recorder.stop()
    }

    // Periodic flushes run on Room's real IO dispatcher, so the published state may lag the virtual clock.
    private suspend fun RideRecorder.awaitSignal(expected: GpsSignal) {
        state.first { it is RecorderState.Active && it.signal == expected }
    }
}
