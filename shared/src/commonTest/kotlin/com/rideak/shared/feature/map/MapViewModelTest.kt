package com.rideak.shared.feature.map

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import com.rideak.shared.FakePrivacyZoneRepository
import com.rideak.shared.FakeRideRepository
import com.rideak.shared.di.DeveloperSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test

/** The map tab stays alive: it must follow finished rides, not load them once. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val noTracks = GeoJson.lines(emptyList())

    /** Same order as the recorder: the ride is created, its points written, then it is marked finished. */
    private suspend fun FakeRideRepository.finishRide(id: String) {
        val summary = RideSummary(RideId(id), 0L, null, RideStatus.RECORDING, 60_000L, 300.0, 60_000L, 5.0, 0.0)
        createRide(summary)
        // 60 s east at 5 m/s, one fix per second, like a real ride.
        val points = List(60) {
            TrackPoint(GeoPoint(49.0364, 2.0761 + it * 0.0000685), it * 1_000L, horizontalAccuracyMeters = 5f, speedMetersPerSecond = 5f)
        }
        appendPoints(RideId(id), 0, points)
        saveSummary(summary.copy(status = RideStatus.FINISHED, endedAtEpochMillis = 60_000L))
    }

    /** Waits in real time: the overlays are computed on Dispatchers.Default. */
    private suspend fun MapViewModel.awaitState(predicate: (WorldMapUiState) -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(10_000) { state.first { !it.loading && predicate(it) } } }

    @Test
    fun aRideFinishedAfterOpeningTheMapAppearsAndDeletingDataClearsIt() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val rides = FakeRideRepository()
        val vm = MapViewModel(rides, DeveloperSettings(), { emptyList() }, FakePrivacyZoneRepository())
        backgroundScope.launch { vm.state.collect {} }

        vm.awaitState { it.tracks == noTracks }

        rides.finishRide("r1")
        vm.awaitState { it.tracks != noTracks && it.exploredCells > 0 }

        rides.deleteFinishedRides()
        vm.awaitState { it.tracks == noTracks && it.exploredCells == 0 }
    }
}
