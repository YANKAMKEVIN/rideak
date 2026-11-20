package com.rideak.shared.feature.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.exploration.PlaceId
import com.rideak.exploration.TrackPoint
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.places
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class PlaceSection(
    val category: PlaceCategory,
    val discovered: List<Place>,
    val hidden: Int,
    /** Start of the first ride that discovered each place. */
    val discoveredAt: Map<PlaceId, Long> = emptyMap(),
) {
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

    val state: StateFlow<PlacesUiState> = repository.observeFinishedRides().map { rides ->
        val places = regions.places()
        val tracks = rides.sortedBy { it.startedAtEpochMillis }.map { it.startedAtEpochMillis to repository.track(it.id) }
        withContext(Dispatchers.Default) {
            val found = discoveryDates(places, tracks, engine, discoverer)
            PlacesUiState(
                loading = false,
                sections = places.groupBy { it.category }.entries.sortedBy { it.key }.map { (category, list) ->
                    PlaceSection(
                        category,
                        list.filter { place -> place.id in found },
                        list.count { place -> place.id !in found },
                        discoveredAt = list.mapNotNull { place -> found[place.id]?.let { place.id to it } }.toMap(),
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlacesUiState())
}

/**
 * When each place was discovered: the start of the first ride, in [rides] order (oldest first),
 * whose own track discovers it. A place is discovered by the history iff one ride touches it, so
 * this finds the same places as replaying every ride together.
 */
internal fun discoveryDates(
    places: List<Place>,
    rides: List<Pair<Long, List<List<TrackPoint>>>>,
    engine: ExplorationEngine = ExplorationEngine.standard(),
    discoverer: PlaceDiscoverer = PlaceDiscoverer(),
): Map<PlaceId, Long> {
    val dates = mutableMapOf<PlaceId, Long>()
    var remaining = places
    for ((startedAt, track) in rides) {
        if (remaining.isEmpty()) break
        val found = discoverer.discovered(remaining, engine.pathsFor(track))
        found.forEach { dates[it] = startedAt }
        remaining = remaining.filterNot { it.id in found }
    }
    return dates
}
