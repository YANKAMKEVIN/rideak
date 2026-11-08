package com.rideak.shared.feature.map

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import com.rideak.shared.FakePrivacyZoneRepository
import com.rideak.shared.FakeRideRepository
import com.rideak.shared.di.DeveloperSettings
import com.rideak.shared.feature.territory.RegionSource
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MapViewModelTest {
    private val ride = RideSummary(RideId("ride"), 0, 60_000, RideStatus.FINISHED, 60_000, 300.0, 60_000, 5.0, 0.0)
    private val track = List(60) { TrackPoint(GeoPoint(49.0364 + it * 0.000045, 2.0761), it * 1_000L) }

    private fun ComposeUiTest.waitForText(text: String) =
        waitUntil(timeoutMillis = 10_000) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun theMapFollowsFinishedRidesAndErasure() = runComposeUiTest {
        val repository = FakeRideRepository()
        val viewModel = MapViewModel(repository, DeveloperSettings(), RegionSource { emptyList() }, FakePrivacyZoneRepository())
        setContent {
            val state by viewModel.state.collectAsState()
            BasicText(if (state.loading) "loading" else if (state.exploredCells > 0) "explored" else "empty")
        }
        waitForText("empty")

        repository.appendPoints(ride.id, 0, track)
        repository.createRide(ride)
        waitForText("explored")

        repository.deleteFinishedRides()
        waitForText("empty")
    }
}
