package com.rideak.shared.feature.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.SlippyTileGrid
import com.rideak.ride.ProfilePoint
import com.rideak.ride.RideMetrics
import com.rideak.ride.RideMetricsCalculator
import com.rideak.ride.RideSummary
import com.rideak.ride.Spot
import com.rideak.ride.elevationProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RideSummaryUiState(
    val loading: Boolean = true,
    val ride: RideSummary? = null,
    /** Replayed from the stored points: also gives values not stored in the summary (descent). */
    val metrics: RideMetrics = RideMetrics(),
    val segments: List<List<GeoPoint>> = emptyList(),
    val spots: List<Spot> = emptyList(),
    val profile: List<ProfilePoint> = emptyList(),
    val cellsTraversed: Int = 0,
)

class RideSummaryViewModel(rideId: String, repository: RideRepository) : ViewModel() {
    private val _state = MutableStateFlow(RideSummaryUiState())
    val state: StateFlow<RideSummaryUiState> = _state

    init {
        viewModelScope.launch {
            val id = RideId(rideId)
            val ride = repository.ride(id)
            val track = repository.track(id)
            val spots = repository.spots(id)
            _state.value = withContext(Dispatchers.Default) {
                RideSummaryUiState(
                    loading = false,
                    ride = ride,
                    metrics = RideMetricsCalculator().compute(track).metrics,
                    segments = track.map { segment -> segment.map { it.position } },
                    spots = spots,
                    profile = elevationProfile(track),
                    cellsTraversed = ExplorationEngine(SlippyTileGrid()).cellsFor(track.flatten()).size,
                )
            }
        }
    }
}
