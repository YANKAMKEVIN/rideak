package com.rideak.exploration

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot

/** Discovery distances. First guesses, to validate with real rides; changing them replays the history. */
data class DiscoveryRules(
    /** A monument or viewpoint is discovered when passing this close. */
    val pointRadiusMeters: Double = 40.0,
    /** A river is discovered when crossing it or riding this close to it. */
    val lineDistanceMeters: Double = 30.0,
    /** A lake cannot be ridden into: riding along its shore this close discovers it. */
    val shoreDistanceMeters: Double = 30.0,
    /** Spacing of the samples taken along the ridden path. */
    val stepMeters: Double = 10.0,
)

/**
 * Which places a ridden path discovered, per geometry (see [PlaceGeometry]). Works on the cleaned
 * stretches from [ExplorationEngine.pathsFor], so GPS drift, gaps and motorised travel discover
 * nothing either. Pure and deterministic, like the cells.
 */
class PlaceDiscoverer(private val rules: DiscoveryRules = DiscoveryRules()) {

    fun discovered(places: List<Place>, paths: List<List<GeoPoint>>): Set<PlaceId> {
        val samples = paths.flatMap { path ->
            if (path.size == 1) path else path.zipWithNext { a, b -> samplesBetween(a, b, rules.stepMeters).toList() }.flatten()
        }
        if (samples.isEmpty()) return emptySet()
        return places.filterTo(mutableListOf()) { place ->
            val reach = reachMeters(place)
            val box = boundsOf(place.geometry).expandedBy(reach)
            samples.any { it in box && touches(place, it) }
        }.mapTo(LinkedHashSet()) { it.id }
    }

    /** Places discovered by [ride] that no ride [before] it had discovered. */
    fun newlyDiscovered(places: List<Place>, ride: List<List<GeoPoint>>, before: List<List<GeoPoint>>): Set<PlaceId> {
        val ridden = discovered(places, ride)
        if (ridden.isEmpty()) return ridden
        return ridden - discovered(places.filter { it.id in ridden }, before)
    }

    private fun reachMeters(place: Place) = when (place.geometry) {
        is PlaceGeometry.Point -> rules.pointRadiusMeters
        is PlaceGeometry.LineString, is PlaceGeometry.MultiLineString -> rules.lineDistanceMeters
        is PlaceGeometry.Polygon, is PlaceGeometry.MultiPolygon -> if (place.category == PlaceCategory.LAKE) rules.shoreDistanceMeters else 0.0
    }

    private fun touches(place: Place, p: GeoPoint): Boolean = when (val g = place.geometry) {
        is PlaceGeometry.Point -> p.distanceTo(g.position) <= rules.pointRadiusMeters
        is PlaceGeometry.LineString -> nearLine(p, g.points, rules.lineDistanceMeters)
        is PlaceGeometry.MultiLineString -> g.lines.any { nearLine(p, it.points, rules.lineDistanceMeters) }
        is PlaceGeometry.Polygon -> inArea(place.category, g, p)
        is PlaceGeometry.MultiPolygon -> g.polygons.any { inArea(place.category, it, p) }
    }

    private fun inArea(category: PlaceCategory, polygon: PlaceGeometry.Polygon, p: GeoPoint): Boolean =
        p in polygon || (category == PlaceCategory.LAKE && nearLine(p, polygon.exterior + polygon.exterior.first(), rules.shoreDistanceMeters))

    private fun nearLine(p: GeoPoint, line: List<GeoPoint>, meters: Double) =
        line.zipWithNext().any { (a, b) -> p.distanceToSegment(a, b) <= meters }
}

/** Shortest distance to the segment [a]–[b], on a local flat projection (fine over a few km). */
fun GeoPoint.distanceToSegment(a: GeoPoint, b: GeoPoint): Double {
    val metersPerDegLat = 111_195.0
    val metersPerDegLon = metersPerDegLat * cos(latitude * PI / 180)
    val px = (longitude - a.longitude) * metersPerDegLon
    val py = (latitude - a.latitude) * metersPerDegLat
    val bx = (b.longitude - a.longitude) * metersPerDegLon
    val by = (b.latitude - a.latitude) * metersPerDegLat
    val len2 = bx * bx + by * by
    val t = if (len2 == 0.0) 0.0 else ((px * bx + py * by) / len2).coerceIn(0.0, 1.0)
    return hypot(px - t * bx, py - t * by)
}

/** Bounding box of any geometry. */
fun boundsOf(geometry: PlaceGeometry): GeoBounds {
    val points = when (geometry) {
        is PlaceGeometry.Point -> listOf(geometry.position)
        is PlaceGeometry.LineString -> geometry.points
        is PlaceGeometry.MultiLineString -> geometry.lines.flatMap { it.points }
        is PlaceGeometry.Polygon -> geometry.exterior
        is PlaceGeometry.MultiPolygon -> geometry.polygons.flatMap { it.exterior }
    }
    return GeoBounds(
        south = points.minOf { it.latitude }, west = points.minOf { it.longitude },
        north = points.maxOf { it.latitude }, east = points.maxOf { it.longitude },
    )
}

private fun GeoBounds.expandedBy(meters: Double): GeoBounds {
    val dLat = meters / 111_195.0
    val dLon = meters / (111_195.0 * cos(((south + north) / 2) * PI / 180))
    return GeoBounds(south - dLat, west - dLon, north + dLat, east + dLon)
}

private operator fun GeoBounds.contains(p: GeoPoint) =
    p.latitude in south..north && p.longitude in west..east
