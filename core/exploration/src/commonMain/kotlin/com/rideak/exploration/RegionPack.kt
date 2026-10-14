package com.rideak.exploration

import kotlin.math.roundToLong

/**
 * Cells of [boundary] reachable by bike: every cell crossed by a rideable [ways] polyline, counting
 * only the stretches inside the boundary. This is the denominator of a region's progress, so a
 * lake, a railway yard or a private estate never makes 100 % unreachable.
 */
fun rideableCells(
    boundary: List<PlaceGeometry.Polygon>,
    ways: List<List<GeoPoint>>,
    grid: CellGrid,
    stepMeters: Double = 10.0,
): Set<CellId> {
    val inside = { p: GeoPoint -> boundary.any { p in it } }
    return ways.flatMapTo(LinkedHashSet()) { grid.cellsAlong(it, stepMeters, inside) }
}

/**
 * Text encoding of a precomputed [ExplorationRegion] ("region pack"), shipped with the app or
 * downloaded later. Plain lines, no dependency, readable in a diff:
 *
 * ```
 * RIDEAK-REGION 1
 * id osm-relation-120955
 * name Cergy
 * grid tile17
 * attribution © OpenStreetMap contributors (ODbL)
 * ring 49.05,2.03;49.06,2.04;…      exterior ring of one polygon
 * hole 49.05,2.03;…                 hole of the polygon above (optional, repeatable)
 * cells 66291/44992 66292/44992 …   cell ids without the "grid/" prefix
 * place <id>\t<CATEGORY>\t<name>\t<geometry>   zero or more, tab-separated
 * ```
 *
 * Place geometry: `P lat,lon` (point), `L ring|ring` (one or more lines),
 * `A exterior#hole|exterior` (one or more polygons, holes after `#`), coordinates as in rings.
 */
object RegionPackCodec {
    private const val HEADER = "RIDEAK-REGION 1"

    data class Pack(val region: ExplorationRegion, val attribution: String, val places: List<Place> = emptyList())

    fun encode(region: ExplorationRegion, attribution: String, places: List<Place> = emptyList()): String = buildString {
        appendLine(HEADER)
        appendLine("id ${region.id.value}")
        appendLine("name ${region.name}")
        appendLine("grid ${region.gridId}")
        appendLine("attribution $attribution")
        region.boundary.forEach { polygon ->
            appendLine("ring ${polygon.exterior.encode()}")
            polygon.holes.forEach { appendLine("hole ${it.encode()}") }
        }
        val prefix = "${region.gridId}/"
        appendLine("cells " + region.rideableCells.map { it.value.removePrefix(prefix) }.sorted().joinToString(" "))
        places.forEach { place ->
            require('\t' !in place.name && '\n' !in place.name) { "unsupported place name: ${place.name}" }
            appendLine("place ${place.id.value}\t${place.category}\t${place.name}\t${place.geometry.encode()}")
        }
    }

    fun decode(text: String): Pack {
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        require(lines.firstOrNull() == HEADER) { "not a RIDEAK region pack (v1)" }
        val fields = mutableMapOf<String, String>()
        val polygons = mutableListOf<PlaceGeometry.Polygon>()
        val places = mutableListOf<Place>()
        for (line in lines.drop(1)) {
            val key = line.substringBefore(' ')
            val value = line.substringAfter(' ', "")
            when (key) {
                "ring" -> polygons += PlaceGeometry.Polygon(value.decodeRing())
                "place" -> {
                    val (id, category, name, geometry) = value.split('\t').also { require(it.size == 4) { "bad place: $line" } }
                    places += Place(PlaceId(id), name, PlaceCategory.valueOf(category), geometry.decodeGeometry())
                }
                "hole" -> {
                    val last = requireNotNull(polygons.removeLastOrNull()) { "hole before any ring" }
                    polygons += last.copy(holes = last.holes + listOf(value.decodeRing()))
                }
                else -> fields[key] = value
            }
        }
        fun field(name: String) = requireNotNull(fields[name]) { "missing '$name'" }
        val gridId = field("grid")
        val region = ExplorationRegion(
            id = RegionId(field("id")),
            name = field("name"),
            boundary = polygons,
            gridId = gridId,
            rideableCells = field("cells").split(' ').filter { it.isNotEmpty() }.mapTo(LinkedHashSet()) { CellId("$gridId/$it") },
        )
        return Pack(region, field("attribution"), places)
    }

    private fun List<GeoPoint>.encode() = joinToString(";") { "${it.latitude.round6()},${it.longitude.round6()}" }

    private fun PlaceGeometry.encode(): String = when (this) {
        is PlaceGeometry.Point -> "P " + listOf(position).encode()
        is PlaceGeometry.LineString -> "L " + points.encode()
        is PlaceGeometry.MultiLineString -> "L " + lines.joinToString("|") { it.points.encode() }
        is PlaceGeometry.Polygon -> "A " + encodeArea()
        is PlaceGeometry.MultiPolygon -> "A " + polygons.joinToString("|") { it.encodeArea() }
    }

    private fun PlaceGeometry.Polygon.encodeArea() = (listOf(exterior) + holes).joinToString("#") { it.encode() }

    private fun String.decodeGeometry(): PlaceGeometry {
        val body = substringAfter(' ')
        return when (substringBefore(' ')) {
            "P" -> PlaceGeometry.Point(body.decodeRing().single())
            "L" -> body.split('|').map { PlaceGeometry.LineString(it.decodeRing()) }.let { it.singleOrNull() ?: PlaceGeometry.MultiLineString(it) }
            "A" -> body.split('|').map { area ->
                val rings = area.split('#').map { it.decodeRing() }
                PlaceGeometry.Polygon(rings.first(), rings.drop(1))
            }.let { it.singleOrNull() ?: PlaceGeometry.MultiPolygon(it) }
            else -> throw IllegalArgumentException("unknown geometry: ${take(20)}")
        }
    }

    private fun String.decodeRing() = split(';').map { pair ->
        GeoPoint(pair.substringBefore(',').toDouble(), pair.substringAfter(',').toDouble())
    }

    // ~10 cm: plenty for a boundary drawn on a phone.
    private fun Double.round6() = (this * 1e6).roundToLong() / 1e6
}
