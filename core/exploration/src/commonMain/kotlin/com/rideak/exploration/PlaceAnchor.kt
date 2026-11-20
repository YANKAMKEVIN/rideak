package com.rideak.exploration

/**
 * One point to pin a place on a map or to measure how close something is to it: the point itself,
 * the middle vertex of a line (of its longest line), the average of a polygon's outer ring (of its
 * largest polygon). Good enough for a marker; not a true centroid.
 */
val Place.anchor: GeoPoint
    get() = when (val g = geometry) {
        is PlaceGeometry.Point -> g.position
        is PlaceGeometry.LineString -> g.points[g.points.size / 2]
        is PlaceGeometry.MultiLineString -> g.lines.maxBy { it.points.size }.points.let { it[it.size / 2] }
        is PlaceGeometry.Polygon -> g.exterior.average()
        is PlaceGeometry.MultiPolygon -> g.polygons.maxBy { it.exterior.size }.exterior.average()
    }

private fun List<GeoPoint>.average() = GeoPoint(sumOf { it.latitude } / size, sumOf { it.longitude } / size)
