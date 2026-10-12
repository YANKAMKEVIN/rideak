package com.rideak.shared.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.data.recorder.RecorderState
import com.rideak.data.recorder.RideRecorder
import com.rideak.exploration.ExplorationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class HomeUiState(
    val totalDistanceMeters: Double = 0.0,
    val rideCount: Int = 0,
    val spotCount: Int = 0,
    val exploredCells: Int = 0,
    val activeRide: RecorderState.Active? = null,
)

class HomeViewModel(repository: RideRepository, recorder: RideRecorder) : ViewModel() {
    private val engine = ExplorationEngine.standard()

    private val totals = repository.observeFinishedRides().map { rides ->
        // Exploration is derived from the stored points every time: nothing to keep in sync.
        val cells = withContext(Dispatchers.Default) {
            engine.cellsForSegments(repository.finishedSegments()).size
        }
        HomeUiState(
            totalDistanceMeters = rides.sumOf { it.distanceMeters },
            rideCount = rides.size,
            spotCount = rides.sumOf { it.spotCount },
            exploredCells = cells,
        )
    }

    val state: StateFlow<HomeUiState> = combine(totals, recorder.state) { totals, recording ->
        totals.copy(activeRide = recording as? RecorderState.Active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
