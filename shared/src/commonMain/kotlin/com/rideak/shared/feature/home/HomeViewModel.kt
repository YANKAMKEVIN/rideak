package com.rideak.shared.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.data.recorder.RecorderState
import com.rideak.data.recorder.RideRecorder
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.shared.feature.map.Counter
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.places
import com.rideak.shared.feature.territory.TerritoryProgress
import com.rideak.shared.feature.territory.regions
import com.rideak.shared.feature.territory.territoryOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/** The four shortcuts of the home screen, and the place categories each one opens in "Lieux". */
enum class HomeCategory(val label: String, val categories: Set<PlaceCategory>) {
    PARKS("Parcs", setOf(PlaceCategory.PARK)),
    RIVERS("Rivières", setOf(PlaceCategory.RIVER, PlaceCategory.LAKE)),
    CITIES("Villes", setOf(PlaceCategory.MONUMENT, PlaceCategory.LANDMARK)),
    HILLS("Collines", setOf(PlaceCategory.VIEWPOINT)),
}

data class HomeUiState(
    val totalDistanceMeters: Double = 0.0,
    val rideCount: Int = 0,
    val spotCount: Int = 0,
    val exploredCells: Int = 0,
    /** Progress in the rider's main region, once they have explored part of one. */
    val territory: TerritoryProgress? = null,
    val activeRide: RecorderState.Active? = null,
    /** Places discovered / known for each home shortcut. */
    val categories: Map<HomeCategory, Counter> = emptyMap(),
)

class HomeViewModel(repository: RideRepository, recorder: RideRecorder, regions: RegionSource) : ViewModel() {
    private val engine = ExplorationEngine.standard()
    private val discoverer = PlaceDiscoverer()

    private val totals = repository.observeFinishedRides().map { rides ->
        // Exploration is derived from the stored points every time: nothing to keep in sync.
        val segments = repository.finishedSegments()
        val places = regions.places()
        val (cells, found) = withContext(Dispatchers.Default) {
            engine.cellsForSegments(segments) to discoverer.discovered(places, engine.pathsFor(segments))
        }
        val territory = territoryOf(regions.regions(), cells)
        HomeUiState(
            totalDistanceMeters = rides.sumOf { it.distanceMeters },
            rideCount = rides.size,
            spotCount = rides.sumOf { it.spotCount },
            exploredCells = cells.size,
            territory = territory,
            categories = HomeCategory.entries.associateWith { shortcut ->
                val known = places.filter { it.category in shortcut.categories }
                Counter(known.count { it.id in found }, known.size)
            },
        )
    }

    val state: StateFlow<HomeUiState> = combine(totals, recorder.state) { totals, recording ->
        totals.copy(activeRide = recording as? RecorderState.Active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
