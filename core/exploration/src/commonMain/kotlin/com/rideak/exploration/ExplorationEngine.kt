package com.rideak.exploration

/**
 * A validation / cleaning step applied to a whole track before cells are computed.
 * Track-level (not point-level) so future rules can look at neighbours: GPS drift while
 * stopped, sustained motorised speed, anti-cheat… Thresholds are constructor parameters,
 * never hardcoded business rules.
 */
fun interface TrackFilter {
    fun apply(track: List<TrackPoint>): List<TrackPoint>
}

/** Drops samples whose reported accuracy is worse than [maxAccuracyMeters]. Unknown accuracy is kept. */
class AccuracyFilter(private val maxAccuracyMeters: Float) : TrackFilter {
    override fun apply(track: List<TrackPoint>): List<TrackPoint> =
        track.filter { point -> point.horizontalAccuracyMeters?.let { it <= maxAccuracyMeters } ?: true }
}

/**
 * Turns raw TrackPoints into the set of cells they touched. Pure and deterministic, so the whole
 * history can be recomputed at any time.
 *
 * J0 limits (intentional, see docs/exploration-model.md):
 * - one cell per sample: the segment between two distant samples is NOT interpolated yet,
 *   so a sparse track can skip cells;
 * - no drift, speed or anti-cheat filtering unless a [TrackFilter] is supplied.
 */
class ExplorationEngine(
    private val grid: CellGrid,
    private val filters: List<TrackFilter> = emptyList(),
) {
    fun cellsFor(track: List<TrackPoint>): Set<CellId> =
        filters.fold(track) { points, filter -> filter.apply(points) }
            .mapTo(LinkedHashSet()) { grid.cellAt(it.position) }
}
