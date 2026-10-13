package com.rideak.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rideak.data.db.RideakDatabase
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RideRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder<RideakDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val repository = RoomRideRepository(database.rideDao())

    @AfterTest
    fun close() = database.close()

    private suspend fun ride(id: String, startedAt: Long, status: RideStatus = RideStatus.FINISHED, segments: Int = 1) {
        repository.createRide(RideSummary(RideId(id), startedAt, startedAt + 60_000, status, 60_000, 0.0, 0, 0.0, 0.0))
        repeat(segments) { segment ->
            repository.appendPoints(RideId(id), segment, listOf(TrackPoint(GeoPoint(49.0, 2.0), startedAt + segment)))
        }
    }

    @Test
    fun segmentsBeforeOnlyIncludeEarlierFinishedRides() = runTest {
        ride("first", startedAt = 1_000, segments = 2)
        ride("abandoned", startedAt = 2_000, status = RideStatus.RECORDING)
        ride("current", startedAt = 3_000)
        ride("later", startedAt = 4_000)

        val before = repository.finishedSegmentsBefore(3_000)

        assertEquals(listOf(1_000L, 1_001L), before.map { it.single().timestampEpochMillis })
        assertEquals(4, repository.finishedSegments().size) // abandoned ride excluded
    }
}
