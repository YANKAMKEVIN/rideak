package com.rideak.tools.regionpack

import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.RegionId
import com.rideak.exploration.RegionPackCodec
import com.rideak.exploration.SlippyTileGrid
import com.rideak.exploration.contains
import com.rideak.exploration.distanceToSegment
import com.rideak.exploration.rideableCells
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File

/**
 * Input: an Overpass `out geom` response with one administrative boundary relation and the
 * highways inside it (query in docs/osm-data-strategy.md). Output: a region pack.
 */
fun main(args: Array<String>) {
    require(args.size in 2..3) { "usage: <roads.json> <out.region> [places.json]" }
    fun elementsOf(path: String) = Json.parseToJsonElement(File(path).readText()).jsonObject.getValue("elements").jsonArray.map { it.jsonObject }
    val elements = elementsOf(args[0])

    val relation = elements.single { it.string("type") == "relation" }
    val boundary = boundaryOf(relation).map { it.simplified(toleranceMeters = 15.0) }
    val ways = elements
        .filter { it.string("type") == "way" && isRideable(it.tags()) }
        .map { way -> way.getValue("geometry").jsonArray.map { it.jsonObject.point() } }

    val grid = SlippyTileGrid()
    val region = ExplorationRegion(
        id = RegionId("osm-relation-${relation.getValue("id").jsonPrimitive.long}"),
        name = relation.tags().getValue("name"),
        boundary = boundary,
        gridId = grid.id,
        rideableCells = rideableCells(boundary, ways, grid),
    )
    val places = args.getOrNull(2)?.let { placesOf(elementsOf(it), boundary) }.orEmpty()
    File(args[1]).writeText(RegionPackCodec.encode(region, "© OpenStreetMap contributors (ODbL)", places))
    println("${region.name}: ${boundary.size} polygon(s), ${boundary.sumOf { it.exterior.size }} boundary points, " +
        "${ways.size} rideable ways, ${region.rideableCells.size} rideable cells, ${places.size} places")
    places.groupBy { it.category }.forEach { (category, list) -> println("  $category: ${list.joinToString { it.name }}") }
}

private val roads = setOf(
    "cycleway", "path", "track", "living_street", "residential", "unclassified",
    "tertiary", "secondary", "primary", "service",
)
private val bikeOnlyIfAllowed = setOf("footway", "pedestrian", "bridleway", "steps")
private val bikeAllowed = setOf("yes", "designated", "permissive")

/** Rideable by bike, from OSM tags. Motorways and trunks are never in [roads]. */
fun isRideable(tags: Map<String, String>): Boolean {
    val highway = tags["highway"] ?: return false
    val bicycle = tags["bicycle"]
    if (bicycle == "no" || bicycle == "dismount") return false
    if (bicycle in bikeAllowed) return highway in roads || highway in bikeOnlyIfAllowed
    if (tags["access"] in setOf("no", "private")) return false
    if (highway == "service" && tags["service"] == "driveway") return false
    return highway in roads
}

/** Outer rings joined from the relation's member ways, each with the inner rings it contains. */
fun boundaryOf(relation: JsonObject): List<PlaceGeometry.Polygon> {
    val members = relation.getValue("members").jsonArray.map { it.jsonObject }.filter { it.string("type") == "way" }
    fun rings(role: String) = joinRings(
        members.filter { it.string("role") == role }.map { m -> m.getValue("geometry").jsonArray.map { it.jsonObject.point() } },
    )
    val inners = rings("inner")
    return rings("outer").map { outer ->
        val polygon = PlaceGeometry.Polygon(outer)
        polygon.copy(holes = inners.filter { it.first() in polygon })
    }
}

/** Joins way fragments end to end into closed rings (first point not repeated). */
fun joinRings(fragments: List<List<GeoPoint>>): List<List<GeoPoint>> {
    val left = fragments.filter { it.size >= 2 }.toMutableList()
    val rings = mutableListOf<List<GeoPoint>>()
    while (left.isNotEmpty()) {
        val ring = left.removeAt(0).toMutableList()
        while (ring.first() != ring.last()) {
            val i = left.indexOfFirst { it.first() == ring.last() || it.last() == ring.last() }
            check(i >= 0) { "boundary ring does not close" }
            val next = left.removeAt(i)
            ring += (if (next.first() == ring.last()) next else next.reversed()).drop(1)
        }
        rings += ring.dropLast(1)
    }
    return rings
}

/** Douglas–Peucker; keeps at least a triangle. */
fun PlaceGeometry.Polygon.simplified(toleranceMeters: Double): PlaceGeometry.Polygon {
    fun simplify(ring: List<GeoPoint>): List<GeoPoint> {
        val closed = ring + ring.first()
        val kept = BooleanArray(closed.size).also { it[0] = true; it[it.lastIndex] = true }
        fun recurse(from: Int, to: Int) {
            var worst = -1; var worstDist = toleranceMeters
            for (i in from + 1 until to) {
                val d = closed[i].distanceToSegment(closed[from], closed[to])
                if (d > worstDist) { worst = i; worstDist = d }
            }
            if (worst >= 0) { kept[worst] = true; recurse(from, worst); recurse(worst, to) }
        }
        recurse(0, closed.lastIndex)
        val result = closed.filterIndexed { i, _ -> kept[i] }.dropLast(1)
        return if (result.size >= 3) result else ring
    }
    return PlaceGeometry.Polygon(simplify(exterior), holes.map(::simplify))
}

