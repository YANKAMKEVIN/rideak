package com.rideak.exploration

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A sensitive area (home, work…) around which ride data must be protected. The rider always sees
 * everything on their own map; whatever is shown to others (share mode, export, future social)
 * goes through [PrivacyMask]. Decisions: docs/privacy.md.
 */
data class PrivacyZone(
    val id: String,
    val label: String,
    val center: GeoPoint,
    val radiusMeters: Double,
) {
    init {
        require(radiusMeters > 0) { "radius must be positive" }
    }

    operator fun contains(point: GeoPoint): Boolean = center.distanceTo(point) <= radiusMeters

    companion object {
        /**
         * A zone that protects [secret] without being centred on it: a circle centred on the home
         * would give the home away by triangulation of truncated tracks. The centre moves 15–40 %
         * of the radius in a random direction and the radius grows by up to 25 %, so [secret] stays
         * well inside. Only the result is stored, never [secret] itself.
         */
        fun around(id: String, label: String, secret: GeoPoint, radiusMeters: Double, random: Random = Random.Default): PrivacyZone {
            val radius = radiusMeters * random.nextDouble(1.0, 1.25)
            val shift = radius * random.nextDouble(0.15, 0.40)
            val bearing = random.nextDouble(0.0, 2 * PI)
            return PrivacyZone(id, label, secret.offset(northMeters = shift * cos(bearing), eastMeters = shift * sin(bearing)), radius)
        }
    }
}

/** Removes what lies in privacy zones from anything shown to someone else. */
class PrivacyMask(private val zones: List<PrivacyZone>) {
    fun hides(point: GeoPoint): Boolean = zones.any { point in it }

    /** Lines cut where they enter a zone: the hidden part disappears, the rest becomes separate lines. */
    fun lines(segments: List<List<GeoPoint>>): List<List<GeoPoint>> {
        if (zones.isEmpty()) return segments
        val visible = mutableListOf<List<GeoPoint>>()
        for (segment in segments) {
            var current = mutableListOf<GeoPoint>()
            for (point in segment) {
                if (hides(point)) {
                    if (current.isNotEmpty()) visible += current
                    current = mutableListOf()
                } else {
                    current += point
                }
            }
            if (current.isNotEmpty()) visible += current
        }
        return visible
    }

    fun points(points: List<GeoPoint>): List<GeoPoint> = points.filterNot(::hides)

    /** Cells touching a zone are hidden too: explored cells alone would draw the outline of a home. */
    fun cells(cells: Collection<CellId>, grid: CellGrid): List<CellId> = cells.filter { cell ->
        val b = grid.boundsOf(cell) ?: return@filter true
        val center = GeoPoint((b.south + b.north) / 2, (b.west + b.east) / 2)
        val halfDiagonal = center.distanceTo(GeoPoint(b.north, b.east))
        zones.none { it.center.distanceTo(center) <= it.radiusMeters + halfDiagonal }
    }
}

/** A place where [rides] rides started or ended. */
data class PrivacySuggestion(val position: GeoPoint, val rides: Int)

/**
 * Places worth protecting: where rides often start or end (home, work), not yet in a zone.
 * Endpoints closer than [clusterMeters] are grouped; a group of at least [minRides] endpoints is
 * suggested, most frequent first.
 */
fun suggestPrivacySpots(
    endpoints: List<GeoPoint>,
    existing: List<PrivacyZone>,
    clusterMeters: Double = 150.0,
    minRides: Int = 2,
): List<PrivacySuggestion> {
    val clusters = mutableListOf<MutableList<GeoPoint>>()
    for (point in endpoints) {
        val cluster = clusters.firstOrNull { it.centroid().distanceTo(point) <= clusterMeters }
        if (cluster != null) cluster += point else clusters += mutableListOf(point)
    }
    return clusters
        .filter { it.size >= minRides }
        .sortedByDescending { it.size }
        .map { PrivacySuggestion(it.centroid(), it.size) }
        .filter { spot -> existing.none { spot.position in it } }
}

private fun List<GeoPoint>.centroid() = GeoPoint(sumOf { it.latitude } / size, sumOf { it.longitude } / size)

/** Position moved by a few hundred metres (flat approximation). */
fun GeoPoint.offset(northMeters: Double, eastMeters: Double): GeoPoint = GeoPoint(
    latitude + northMeters / 111_195.0,
    longitude + eastMeters / (111_195.0 * cos(latitude * PI / 180)),
)
