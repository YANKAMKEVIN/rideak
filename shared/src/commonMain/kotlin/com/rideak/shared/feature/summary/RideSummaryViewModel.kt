package com.rideak.shared.feature.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.places
import com.rideak.exploration.TrackPoint
import com.rideak.exploration.RideId
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
    val segments: List<List<TrackPoint>> = emptyList(),
    val spots: List<Spot> = emptyList(),
    val profile: List<ProfilePoint> = emptyList(),
    val cellsTraversed: Int = 0,
    /** Cells this ride touched that no earlier ride had: the "world revealing itself". */
    val cellsRevealed: Int = 0,
    /** Real places (parks, rivers, monuments…) this ride discovered for the first time. */
    val newPlaces: List<Place> = emptyList(),
)

class RideSummaryViewModel(rideId: String, repository: RideRepository, regions: RegionSource) : ViewModel() {
    private val _state = MutableStateFlow(RideSummaryUiState())
    val state: StateFlow<RideSummaryUiState> = _state

    init {
        viewModelScope.launch {
            val id = RideId(rideId)
            val ride = repository.ride(id)
            val track = repository.track(id)
            val spots = repository.spots(id)
            val before = ride?.let { repository.finishedSegmentsBefore(it.startedAtEpochMillis) }.orEmpty()
            val places = regions.places()
            _state.value = withContext(Dispatchers.Default) {
                val engine = ExplorationEngine.standard()
                RideSummaryUiState(
                    loading = false,
                    ride = ride,
                    metrics = RideMetricsCalculator().compute(track).metrics,
                    segments = track,
                    spots = spots,
                    profile = elevationProfile(track),
                    cellsTraversed = engine.cellsForSegments(track).size,
                    cellsRevealed = engine.revealedBy(track, before).size,
                    newPlaces = PlaceDiscoverer().newlyDiscovered(places, engine.pathsFor(track), engine.pathsFor(before))
                        .let { ids -> places.filter { it.id in ids } },
                )
            }
        }
    }
}
