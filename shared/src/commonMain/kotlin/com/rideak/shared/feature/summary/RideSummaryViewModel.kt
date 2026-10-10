package com.rideak.shared.feature.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.RideId
import com.rideak.exploration.SlippyTileGrid
import com.rideak.ride.RideSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RideSummaryUiState(val ride: RideSummary? = null, val cellsTraversed: Int = 0, val loading: Boolean = true)

class RideSummaryViewModel(rideId: String, repository: RideRepository) : ViewModel() {
    private val _state = MutableStateFlow(RideSummaryUiState())
    val state: StateFlow<RideSummaryUiState> = _state

    init {
        viewModelScope.launch {
            val id = RideId(rideId)
            val ride = repository.ride(id)
            val cells = withContext(Dispatchers.Default) {
                ExplorationEngine(SlippyTileGrid()).cellsFor(repository.track(id).flatten()).size
            }
            _state.value = RideSummaryUiState(ride, cells, loading = false)
        }
    }
}
