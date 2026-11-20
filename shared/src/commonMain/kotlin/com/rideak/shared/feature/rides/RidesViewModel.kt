package com.rideak.shared.feature.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.CellId
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideSummary
import com.rideak.shared.format.formatMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One row of "Mes sorties": the ride, the shape of its track, and the cells it revealed first. */
data class RideListItem(val ride: RideSummary, val outline: List<GeoPoint> = emptyList(), val revealedCells: Int = 0)

/** Rides of one month, most recent first, with their total distance. */
data class RideMonth(val title: String, val totalMeters: Double, val rides: List<RideListItem>)

class RidesViewModel(private val repository: RideRepository) : ViewModel() {
    private val engine = ExplorationEngine.standard()

    /** Null while loading. */
    val months: StateFlow<List<RideMonth>?> = repository.observeFinishedRides().map { rides ->
        val tracks = rides.map { it to repository.track(it.id) }
        withContext(Dispatchers.Default) { groupByMonth(rideListItems(tracks, engine)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(ride: RideSummary) {
        viewModelScope.launch { repository.deleteFinishedRide(ride.id) }
    }
}

/**
 * Replays the rides from the oldest: each one's revealed cells are those no earlier ride touched
 * (same rule as the ride summary). Returned most recent first.
 */
internal fun rideListItems(rides: List<Pair<RideSummary, List<List<TrackPoint>>>>, engine: ExplorationEngine): List<RideListItem> {
    val known = HashSet<CellId>()
    return rides.sortedBy { it.first.startedAtEpochMillis }.map { (ride, track) ->
        val cells = engine.cellsForSegments(track)
        val revealed = cells.count { it !in known }
        known += cells
        RideListItem(ride, outline(track), revealed)
    }.reversed()
}

/** Groups consecutive rides (already most recent first) by calendar month. */
internal fun groupByMonth(items: List<RideListItem>): List<RideMonth> =
    items.groupBy { formatMonth(it.ride.startedAtEpochMillis) }
        .map { (title, rides) -> RideMonth(title, rides.sumOf { it.ride.distanceMeters }, rides) }

/** At most ~60 positions: enough for a thumbnail. */
private fun outline(track: List<List<TrackPoint>>): List<GeoPoint> {
    val points = track.flatten().map { it.position }
    if (points.size <= 60) return points
    val step = points.size / 60.0
    return List(60) { points[(it * step).toInt()] } + points.last()
}
