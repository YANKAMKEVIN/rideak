package com.rideak.tools.regionpack

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import com.rideak.exploration.contains
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Category of a place from its OSM tags, or null if it is not a place RIDEAK shows. */
fun categoryOf(tags: Map<String, String>): PlaceCategory? = when {
    tags["leisure"] == "park" -> PlaceCategory.PARK
    tags["landuse"] == "forest" || tags["natural"] == "wood" -> PlaceCategory.FOREST
    // River areas are skipped: the waterway line already stands for the river.
    tags["natural"] == "water" && tags["water"] !in setOf("river", "canal", "stream", "ditch", "wastewater") -> PlaceCategory.LAKE
    tags["waterway"] == "river" -> PlaceCategory.RIVER
    tags["tourism"] == "viewpoint" -> PlaceCategory.VIEWPOINT
    tags["historic"] != null -> PlaceCategory.MONUMENT
    tags["tourism"] == "attraction" || tags["tourism"] == "artwork" -> if (isBusiness(tags)) null else PlaceCategory.LANDMARK
    else -> null
}

/**
 * A "curiosity" (`tourism=attraction|artwork`) that is really a business or a play area: a ride
 * at the leisure park, a kids' play park, a paid attraction. RIDEAK keeps what a rider discovers
 * by riding past (an artwork, the Axe Majeur), not what one buys a ticket for.
 */
internal fun isBusiness(tags: Map<String, String>): Boolean =
    businessKeys.any { it in tags } ||
        tags["fee"] == "yes" ||
        tags["leisure"] in playOrSportLeisure ||
        tags["attraction"] in rides ||
        tags["amenity"].let { it != null && it !in publicAmenities }

private val businessKeys = setOf("shop", "craft", "office", "brand", "phone", "contact:phone", "email", "contact:email", "sport")
private val playOrSportLeisure = setOf(
    "playground", "amusement_arcade", "adult_gaming_centre", "sports_centre", "fitness_centre", "water_park",
    "trampoline_park", "escape_game", "miniature_golf", "bowling_alley", "ice_rink", "pitch", "horse_riding",
)
private val rides = setOf(
    "amusement_ride", "kiddie_ride", "carousel", "roller_coaster", "water_slide", "bumper_car", "go_kart",
    "summer_toboggan", "big_wheel", "drop_tower", "swing_carousel", "train",
)
/** Amenities an artwork or a landmark can carry without being a business. */
private val publicAmenities = setOf("fountain", "clock", "place_of_worship", "bench")

/**
 * Elements left out by id: commercial attractions of the Île de loisirs de Cergy-Pontoise that
 * slipped into the first Cergy pack ("Aeroscoot", "Baby Park"). Their tags could not be checked
 * against the live OSM data, so [isBusiness] may not catch them; listing them keeps them out of
 * any regenerated pack. Add an id here when a pack shows a shop as a curiosity.
 */
internal val excludedPlaces = setOf("node/6873617685", "node/11611881631")

private val areaCategories = setOf(PlaceCategory.PARK, PlaceCategory.FOREST, PlaceCategory.LAKE)

/**
 * Places from an Overpass `out geom` response, kept when they lie in [boundary] (at least one
 * vertex inside). OSM often splits one real place into several elements (a river into many
 * ways, a park cut by a road): elements with the same name and category become one place.
 */
fun placesOf(elements: List<JsonObject>, boundary: List<PlaceGeometry.Polygon>): List<Place> {
    data class Part(val id: String, val name: String, val category: PlaceCategory, val geometry: PlaceGeometry)

    val parts = elements.mapNotNull { element ->
        val tags = element.tags()
        val category = categoryOf(tags) ?: return@mapNotNull null
        val name = tags["name"] ?: if (category == PlaceCategory.VIEWPOINT) "Point de vue" else return@mapNotNull null
        val type = element.string("type")
        val geometry = when {
            type == "node" -> PlaceGeometry.Point(element.point())
            type == "way" -> {
                val line = element.getValue("geometry").jsonArray.map { it.jsonObject.point() }
                when {
                    category == PlaceCategory.RIVER -> PlaceGeometry.LineString(line)
                    category in areaCategories && line.size >= 4 && line.first() == line.last() ->
                        PlaceGeometry.Polygon(line.dropLast(1)).simplified(toleranceMeters = 8.0)
                    category in areaCategories -> return@mapNotNull null // unclosed area: broken data
                    else -> PlaceGeometry.Point(centroid(line))
                }
            }
            type == "relation" && category in areaCategories ->
                runCatching { boundaryOf(element) }.getOrNull()?.takeIf { it.isNotEmpty() }
                    ?.map { it.simplified(toleranceMeters = 8.0) }
                    ?.let { it.singleOrNull() ?: PlaceGeometry.MultiPolygon(it) }
                    ?: return@mapNotNull null
            else -> return@mapNotNull null
        }
        Part("$type/${element.string("id")}", name.replace('\t', ' '), category, geometry)
    }.filter { it.id !in excludedPlaces }.filter { part -> vertices(part.geometry).any { p -> boundary.any { p in it } } }

    return parts
        .groupBy { if (it.geometry is PlaceGeometry.Point) it.id else "${it.category}/${it.name}" }
        .values
        .map { group ->
            val first = group.minBy { it.id }
            Place(PlaceId(first.id), first.name, first.category, merge(group.map { it.geometry }))
        }
        .sortedWith(compareBy({ it.category }, { it.name }))
}

private fun merge(geometries: List<PlaceGeometry>): PlaceGeometry {
    geometries.singleOrNull()?.let { return it }
    val lines = geometries.flatMap {
        when (it) {
            is PlaceGeometry.LineString -> listOf(it)
            is PlaceGeometry.MultiLineString -> it.lines
            else -> emptyList()
        }
    }
    val polygons = geometries.flatMap {
        when (it) {
            is PlaceGeometry.Polygon -> listOf(it)
            is PlaceGeometry.MultiPolygon -> it.polygons
            else -> emptyList()
        }
    }
    return when {
        lines.isNotEmpty() -> PlaceGeometry.MultiLineString(joinLines(lines.map { it.points }).map(PlaceGeometry::LineString))
        else -> PlaceGeometry.MultiPolygon(polygons)
    }
}

/** Chains line fragments that share an end point, to keep river geometry compact. */
fun joinLines(fragments: List<List<GeoPoint>>): List<List<GeoPoint>> {
    val left = fragments.toMutableList()
    val chains = mutableListOf<List<GeoPoint>>()
    while (left.isNotEmpty()) {
        val chain = left.removeAt(0).toMutableList()
        while (true) {
            val i = left.indexOfFirst { it.first() == chain.last() || it.last() == chain.last() }
            if (i < 0) break
            val next = left.removeAt(i)
            chain += (if (next.first() == chain.last()) next else next.reversed()).drop(1)
        }
        chains += chain
    }
    return chains
}

private fun vertices(geometry: PlaceGeometry): List<GeoPoint> = when (geometry) {
    is PlaceGeometry.Point -> listOf(geometry.position)
    is PlaceGeometry.LineString -> geometry.points
    is PlaceGeometry.MultiLineString -> geometry.lines.flatMap { it.points }
    is PlaceGeometry.Polygon -> geometry.exterior
    is PlaceGeometry.MultiPolygon -> geometry.polygons.flatMap { it.exterior }
}

private fun centroid(points: List<GeoPoint>) =
    GeoPoint(points.map { it.latitude }.average(), points.map { it.longitude }.average())

internal fun JsonObject.string(key: String) = get(key)?.jsonPrimitive?.content
internal fun JsonObject.tags(): Map<String, String> = get("tags")?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
internal fun JsonObject.point() = GeoPoint(
    getValue("lat").jsonPrimitive.content.toDouble(),
    getValue("lon").jsonPrimitive.content.toDouble(),
)
