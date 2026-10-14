package com.rideak.shared.feature.progress

import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.exploration.PlayerStats
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideSummary

/**
 * Player stats for a set of finished rides and their stored points. Pure: the Profile computes it
 * for every ride, the ride summary for the rides before a ride and up to it.
 */
fun playerStats(
    rides: List<RideSummary>,
    segments: List<List<TrackPoint>>,
    regions: List<ExplorationRegion>,
    places: List<Place>,
    engine: ExplorationEngine = ExplorationEngine.standard(),
    discoverer: PlaceDiscoverer = PlaceDiscoverer(),
): PlayerStats {
    val explored = engine.cellsForSegments(segments)
    val found = discoverer.discovered(places, engine.pathsFor(segments))
    return PlayerStats(
        rides = rides.size,
        distanceMeters = rides.sumOf { it.distanceMeters },
        exploredCells = explored.size,
        discoveredPlaces = places.filter { it.id in found }.groupingBy { it.category }.eachCount(),
        exploredRegions = regions.count { it.progress(explored).exploredRideableCells > 0 },
    )
}
