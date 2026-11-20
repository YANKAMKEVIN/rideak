package com.rideak.exploration

/**
 * Follows a ride while it is being recorded: the cells and places it reveals for the first time,
 * compared with every ride [before] it (same rules as the end-of-ride summary). Fed with the
 * growing track, it reports only what is new since the previous [update].
 */
class LiveDiscovery(
    before: List<List<TrackPoint>>,
    places: List<Place>,
    private val engine: ExplorationEngine = ExplorationEngine.standard(),
    private val discoverer: PlaceDiscoverer = PlaceDiscoverer(),
) {
    /** What one [update] found, and the totals of the ride so far. */
    data class Update(val newCells: Int, val newPlaces: List<Place>, val revealedCells: Int, val discoveredPlaces: Int)

    private val known: Set<CellId> = engine.cellsForSegments(before)
    private val hidden: MutableList<Place> = discoverer.discovered(places, engine.pathsFor(before))
        .let { found -> places.filterNot { it.id in found } }.toMutableList()
    private val revealed = HashSet<CellId>()
    private var discovered = 0

    fun update(ride: List<List<TrackPoint>>): Update {
        val cells = engine.cellsForSegments(ride).filterNot { it in known }
        val before = revealed.size
        revealed += cells
        val found = if (hidden.isEmpty()) emptySet() else discoverer.discovered(hidden, engine.pathsFor(ride))
        val newPlaces = hidden.filter { it.id in found }
        hidden.removeAll(newPlaces)
        discovered += newPlaces.size
        return Update(revealed.size - before, newPlaces, revealed.size, discovered)
    }
}
