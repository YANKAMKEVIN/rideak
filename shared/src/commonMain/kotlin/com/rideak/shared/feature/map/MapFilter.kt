package com.rideak.shared.feature.map

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import com.rideak.exploration.PrivacyMask

/** What the carousel puts forward on "Ma carte". Place filters show only places already discovered. */
enum class MapFilter(val categories: Set<PlaceCategory>) {
    ALL(emptySet()),
    CITIES(emptySet()),
    PARKS(setOf(PlaceCategory.PARK)),
    WATER(setOf(PlaceCategory.RIVER, PlaceCategory.LAKE)),
    FORESTS(setOf(PlaceCategory.FOREST)),
    ;

    val showsPlaces: Boolean get() = categories.isNotEmpty()
}

/** Discovered places of one filter, split by how the map draws them. */
data class PlaceOverlay(
    val areas: List<PlaceGeometry.Polygon> = emptyList(),
    val lines: List<List<GeoPoint>> = emptyList(),
    val points: List<GeoPoint> = emptyList(),
    val count: Int = 0,
) {
    val vertices: List<GeoPoint> get() = areas.flatMap { it.exterior } + lines.flatten() + points
}

/**
 * The places [filter] puts on the map: discovered ones only (hidden places stay a surprise), minus
 * any place touching a privacy zone of [mask] (a discovered park next to home would point at it).
 */
fun placeOverlay(places: List<Place>, found: Set<PlaceId>, filter: MapFilter, mask: PrivacyMask): PlaceOverlay {
    val shown = places.filter { it.category in filter.categories && it.id in found && it.geometry.vertices().none(mask::hides) }
    val geometries = shown.flatMap { it.geometry.parts() }
    return PlaceOverlay(
        areas = geometries.filterIsInstance<PlaceGeometry.Polygon>(),
        lines = geometries.filterIsInstance<PlaceGeometry.LineString>().map { it.points },
        points = geometries.filterIsInstance<PlaceGeometry.Point>().map { it.position },
        count = shown.size,
    )
}

/** Simple geometries only: multi-geometries are flattened. */
private fun PlaceGeometry.parts(): List<PlaceGeometry> = when (this) {
    is PlaceGeometry.MultiLineString -> lines
    is PlaceGeometry.MultiPolygon -> polygons
    else -> listOf(this)
}

private fun PlaceGeometry.vertices(): List<GeoPoint> = when (this) {
    is PlaceGeometry.Point -> listOf(position)
    is PlaceGeometry.LineString -> points
    is PlaceGeometry.MultiLineString -> lines.flatMap { it.points }
    is PlaceGeometry.Polygon -> exterior + holes.flatten()
    is PlaceGeometry.MultiPolygon -> polygons.flatMap { it.exterior + it.holes.flatten() }
}

/** South-west and north-east corners of [points], or nothing to fit. */
fun boundsOf(points: List<GeoPoint>): List<GeoPoint> = if (points.isEmpty()) emptyList() else listOf(
    GeoPoint(points.minOf { it.latitude }, points.minOf { it.longitude }),
    GeoPoint(points.maxOf { it.latitude }, points.maxOf { it.longitude }),
)
