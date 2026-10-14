package com.rideak.shared.feature.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.places
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class PlaceSection(val category: PlaceCategory, val discovered: List<Place>, val hidden: Int) {
    val total: Int get() = discovered.size + hidden
}

data class PlacesUiState(
    val loading: Boolean = true,
    val sections: List<PlaceSection> = emptyList(),
) {
    val discovered: Int get() = sections.sumOf { it.discovered.size }
    val total: Int get() = sections.sumOf { it.total }
}

/** Every known place, split into discovered (derived from all finished rides) and still hidden. */
class PlacesViewModel(repository: RideRepository, regions: RegionSource) : ViewModel() {
    private val engine = ExplorationEngine.standard()
    private val discoverer = PlaceDiscoverer()

    val state: StateFlow<PlacesUiState> = repository.observeFinishedRides().map {
        val places = regions.places()
        val segments = repository.finishedSegments()
        withContext(Dispatchers.Default) {
            val found = discoverer.discovered(places, engine.pathsFor(segments))
            PlacesUiState(
                loading = false,
                sections = places.groupBy { it.category }.entries.sortedBy { it.key }.map { (category, list) ->
                    PlaceSection(category, list.filter { place -> place.id in found }, list.count { place -> place.id !in found })
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacesUiState())
}
