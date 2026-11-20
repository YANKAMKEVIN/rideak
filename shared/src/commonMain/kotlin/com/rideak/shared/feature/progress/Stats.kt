package com.rideak.shared.feature.progress

import com.rideak.exploration.Badge
import com.rideak.exploration.CellId
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.exploration.PlaceId
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

/**
 * When each badge was earned: the end of the ride after which it became unlocked, replaying
 * [rides] (each with its own stored points) from the oldest. Same stats as [playerStats],
 * accumulated ride by ride. Badges not earned yet are absent.
 */
fun badgeUnlockDates(
    rides: List<Pair<RideSummary, List<List<TrackPoint>>>>,
    regions: List<ExplorationRegion>,
    places: List<Place>,
    engine: ExplorationEngine = ExplorationEngine.standard(),
    discoverer: PlaceDiscoverer = PlaceDiscoverer(),
): Map<Badge, Long> {
    val explored = HashSet<CellId>()
    val found = HashSet<PlaceId>()
    var distance = 0.0
    val dates = mutableMapOf<Badge, Long>()
    rides.sortedBy { it.first.startedAtEpochMillis }.forEachIndexed { index, (ride, track) ->
        distance += ride.distanceMeters
        explored += engine.cellsForSegments(track)
        found += discoverer.discovered(places.filterNot { it.id in found }, engine.pathsFor(track))
        val stats = PlayerStats(
            rides = index + 1,
            distanceMeters = distance,
            exploredCells = explored.size,
            discoveredPlaces = places.filter { it.id in found }.groupingBy { it.category }.eachCount(),
            exploredRegions = regions.count { it.progress(explored).exploredRideableCells > 0 },
        )
        Badge.entries.filter { it !in dates && it.unlocked(stats) }
            .forEach { dates[it] = ride.endedAtEpochMillis ?: ride.startedAtEpochMillis }
    }
    return dates
}
