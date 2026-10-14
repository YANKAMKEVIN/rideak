package com.rideak.exploration

import kotlin.math.ceil

/**
 * Even-odd ray casting in degrees: exact enough at city scale, where edges are short.
 * A point inside a hole is outside the polygon.
 */
operator fun PlaceGeometry.Polygon.contains(point: GeoPoint): Boolean =
    ringContains(exterior, point) && holes.none { ringContains(it, point) }

private fun ringContains(ring: List<GeoPoint>, point: GeoPoint): Boolean {
    var inside = false
    var j = ring.lastIndex
    for (i in ring.indices) {
        val a = ring[i]
        val b = ring[j]
        if ((a.latitude > point.latitude) != (b.latitude > point.latitude)) {
            val crossLon = a.longitude + (point.latitude - a.latitude) / (b.latitude - a.latitude) * (b.longitude - a.longitude)
            if (point.longitude < crossLon) inside = !inside
        }
        j = i
    }
    return inside
}

/**
 * Positions every [stepMeters] or less along the straight segment from [a] to [b], both ends
 * included. Linear in degrees: indistinguishable from the geodesic over a few hundred metres.
 */
fun samplesBetween(a: GeoPoint, b: GeoPoint, stepMeters: Double): Sequence<GeoPoint> {
    val steps = ceil(a.distanceTo(b) / stepMeters).toInt().coerceAtLeast(1)
    return (0..steps).asSequence().map { k ->
        val f = k.toDouble() / steps
        GeoPoint(a.latitude + (b.latitude - a.latitude) * f, a.longitude + (b.longitude - a.longitude) * f)
    }
}

/** Every cell a polyline passes through, sampled every [stepMeters] (keep it well below a cell's size). */
fun CellGrid.cellsAlong(line: List<GeoPoint>, stepMeters: Double, keep: (GeoPoint) -> Boolean = { true }): Set<CellId> {
    val cells = LinkedHashSet<CellId>()
    if (line.size == 1 && keep(line[0])) cells += cellAt(line[0])
    line.zipWithNext { a, b -> samplesBetween(a, b, stepMeters).filter(keep).mapTo(cells, ::cellAt) }
    return cells
}
