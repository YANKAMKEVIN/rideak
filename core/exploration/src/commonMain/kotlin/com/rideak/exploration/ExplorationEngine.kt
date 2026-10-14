package com.rideak.exploration

/**
 * A validation / cleaning step applied to a whole track before cells are computed.
 * Track-level (not point-level) so rules can look at neighbours: GPS drift while
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
 * GPS drift while stopped: a rider waiting at a light sees fixes wander tens of metres around them
 * and must not explore the neighbouring cells. Keeps a sample only once it is at least
 * [minDisplacementMeters] away from the last kept one. Real movement still registers, just
 * [minDisplacementMeters] late — far below a cell's size.
 */
class DriftFilter(private val minDisplacementMeters: Double) : TrackFilter {
    override fun apply(track: List<TrackPoint>): List<TrackPoint> {
        val kept = ArrayList<TrackPoint>(track.size)
        for (point in track) {
            val last = kept.lastOrNull()
            if (last == null || last.position.distanceTo(point.position) >= minDisplacementMeters) kept += point
        }
        return kept
    }
}

/**
 * Car or train: removes the samples of any stretch ridden faster than [maxSpeedMetersPerSecond]
 * for at least [minDurationMillis]. Speed is measured between positions, never taken from the OS,
 * and a short fast descent stays (it lasts less than [minDurationMillis]). The removed stretch
 * leaves a gap that [ExplorationEngine] will not bridge.
 */
class MotorizedSpeedFilter(
    private val maxSpeedMetersPerSecond: Double,
    private val minDurationMillis: Long,
) : TrackFilter {
    override fun apply(track: List<TrackPoint>): List<TrackPoint> {
        if (track.size < 2) return track
        val drop = BooleanArray(track.size)
        var runStart = -1 // index of the first point of the current fast run
        fun closeRun(end: Int) {
            if (runStart >= 0 && track[end].timestampEpochMillis - track[runStart].timestampEpochMillis >= minDurationMillis) {
                for (i in runStart..end) drop[i] = true
            }
            runStart = -1
        }
        for (i in 1 until track.size) {
            val a = track[i - 1]
            val b = track[i]
            val seconds = (b.timestampEpochMillis - a.timestampEpochMillis) / 1000.0
            val fast = seconds > 0 && a.position.distanceTo(b.position) / seconds > maxSpeedMetersPerSecond
            if (fast) {
                if (runStart < 0) runStart = i - 1
            } else {
                closeRun(i - 1)
            }
        }
        closeRun(track.lastIndex)
        return track.filterIndexed { i, _ -> !drop[i] }
    }
}

/**
 * When two consecutive samples are far apart, the cells between them are explored too —
 * unless the gap looks like lost signal or removed motorised travel.
 */
data class Interpolation(
    /** Longest gap, in time, that is still bridged. */
    val maxGapMillis: Long,
    /** Longest gap, in distance, that is still bridged. */
    val maxGapMeters: Double,
    /** Spacing of the intermediate samples; keep it well below a cell's size. */
    val stepMeters: Double,
)

/**
 * Turns raw TrackPoints into the set of cells they touched. Pure and deterministic, so the whole
 * history can be recomputed at any time.
 *
 * Filters run first, in order. Then each sample explores its cell and, with [interpolation], so
 * do the cells along the straight line to the next sample when the gap is small enough. Separate
 * recording segments (pauses) are never joined: pass them to [cellsForSegments].
 */
class ExplorationEngine(
    private val grid: CellGrid,
    private val filters: List<TrackFilter> = emptyList(),
    private val interpolation: Interpolation? = null,
) {
    fun cellsFor(track: List<TrackPoint>): Set<CellId> = cellsForSegments(listOf(track))

    fun cellsForSegments(segments: List<List<TrackPoint>>): Set<CellId> {
        val cells = LinkedHashSet<CellId>()
        for (path in pathsFor(segments)) {
            if (path.size == 1) cells += grid.cellAt(path[0])
            path.zipWithNext { a, b -> samplesBetween(a, b, interpolation?.stepMeters ?: Double.MAX_VALUE).mapTo(cells, grid::cellAt) }
        }
        return cells
    }

    /**
     * Cells a ride revealed for the first time: touched by [ride], never by any ride [before] it.
     * Derived on demand like everything else, so replaying the history keeps it consistent.
     */
    fun revealedBy(ride: List<List<TrackPoint>>, before: List<List<TrackPoint>>): Set<CellId> =
        cellsForSegments(ride) - cellsForSegments(before)

    /**
     * Where the rider really went: filtered samples, split into continuous stretches wherever a
     * gap must not be bridged (pause, lost signal, removed motorised travel, or no [interpolation]).
     * Cells and place discoveries are both derived from these stretches.
     */
    fun pathsFor(segments: List<List<TrackPoint>>): List<List<GeoPoint>> {
        val paths = mutableListOf<List<GeoPoint>>()
        for (segment in segments) {
            val points = filters.fold(segment) { points, filter -> filter.apply(points) }
            var current = mutableListOf<GeoPoint>()
            points.forEachIndexed { i, point ->
                if (i > 0 && !bridgeable(points[i - 1], point)) {
                    paths += current
                    current = mutableListOf()
                }
                current += point.position
            }
            if (current.isNotEmpty()) paths += current
        }
        return paths
    }

    private fun bridgeable(a: TrackPoint, b: TrackPoint): Boolean {
        val rule = interpolation ?: return false
        return a.position.distanceTo(b.position) <= rule.maxGapMeters &&
            b.timestampEpochMillis - a.timestampEpochMillis <= rule.maxGapMillis
    }

    companion object {
        /**
         * The rules used everywhere in the app. Every threshold is a first guess to be validated
         * against real rides — change them here and the whole history is recomputed.
         */
        fun standard(grid: CellGrid = SlippyTileGrid()) = ExplorationEngine(
            grid = grid,
            filters = listOf(
                AccuracyFilter(maxAccuracyMeters = 50f),
                MotorizedSpeedFilter(maxSpeedMetersPerSecond = 45 / 3.6, minDurationMillis = 60_000),
                DriftFilter(minDisplacementMeters = 15.0),
            ),
            interpolation = Interpolation(maxGapMillis = 60_000, maxGapMeters = 400.0, stepMeters = 10.0),
        )
    }
}
