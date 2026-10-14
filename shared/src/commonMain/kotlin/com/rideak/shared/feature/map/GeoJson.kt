package com.rideak.shared.feature.map

import com.rideak.exploration.GeoBounds
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PrivacyZone
import com.rideak.exploration.offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Minimal GeoJSON writers for the map overlays (no extra dependency).
 * Coordinates are [longitude, latitude] as the spec requires.
 */
internal object GeoJson {
    private fun StringBuilder.position(longitude: Double, latitude: Double) {
        append('[').append(longitude).append(',').append(latitude).append(']')
    }

    private fun StringBuilder.ring(bounds: GeoBounds, clockwise: Boolean) {
        val corners = listOf(
            bounds.west to bounds.south, bounds.east to bounds.south,
            bounds.east to bounds.north, bounds.west to bounds.north,
        ).let { if (clockwise) it.reversed() else it }
        append('[')
        (corners + corners.first()).forEachIndexed { i, (lon, lat) ->
            if (i > 0) append(',')
            position(lon, lat)
        }
        append(']')
    }

    private fun feature(geometry: String) =
        """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{},"geometry":$geometry}]}"""

    fun lines(segments: List<List<GeoPoint>>): String = feature(
        buildString {
            append("""{"type":"MultiLineString","coordinates":[""")
            segments.filter { it.size > 1 }.forEachIndexed { i, segment ->
                if (i > 0) append(',')
                append('[')
                segment.forEachIndexed { j, p -> if (j > 0) append(','); position(p.longitude, p.latitude) }
                append(']')
            }
            append("]}")
        },
    )

    fun points(points: List<GeoPoint>): String = feature(
        buildString {
            append("""{"type":"MultiPoint","coordinates":[""")
            points.forEachIndexed { i, p -> if (i > 0) append(','); position(p.longitude, p.latitude) }
            append("]}")
        },
    )

    fun squares(cells: List<GeoBounds>): String = feature(
        buildString {
            append("""{"type":"MultiPolygon","coordinates":[""")
            cells.forEachIndexed { i, b -> if (i > 0) append(','); append('['); ring(b, clockwise = false); append(']') }
            append("]}")
        },
    )

    /** The "fog": the whole world minus the explored cells (holes). */
    fun fog(explored: List<GeoBounds>): String = feature(
        buildString {
            append("""{"type":"Polygon","coordinates":[""")
            ring(GeoBounds(south = -85.0, west = -180.0, north = 85.0, east = 180.0), clockwise = false)
            explored.forEach { append(','); ring(it, clockwise = true) }
            append("]}")
        },
    )

    /** Privacy zones as 48-sided polygons. */
    fun circles(zones: List<PrivacyZone>): String = feature(
        buildString {
            append("""{"type":"MultiPolygon","coordinates":[""")
            zones.forEachIndexed { i, zone ->
                if (i > 0) append(',')
                append("[[")
                (0..48).forEach { k ->
                    if (k > 0) append(',')
                    val angle = 2 * PI * k / 48
                    val p = zone.center.offset(northMeters = zone.radiusMeters * cos(angle), eastMeters = zone.radiusMeters * sin(angle))
                    position(p.longitude, p.latitude)
                }
                append("]]")
            }
            append("]}")
        },
    )
}
