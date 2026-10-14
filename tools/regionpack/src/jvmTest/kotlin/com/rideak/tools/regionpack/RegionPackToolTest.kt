package com.rideak.tools.regionpack

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PlaceGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import com.rideak.exploration.PlaceCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.assertTrue

class RegionPackToolTest {
    @Test
    fun rideableTags() {
        assertTrue(isRideable(mapOf("highway" to "residential")))
        assertTrue(isRideable(mapOf("highway" to "cycleway")))
        assertTrue(isRideable(mapOf("highway" to "footway", "bicycle" to "designated")))
        assertFalse(isRideable(mapOf("highway" to "footway")), "pedestrian only by default in France")
        assertFalse(isRideable(mapOf("highway" to "motorway")))
        assertFalse(isRideable(mapOf("highway" to "residential", "access" to "private")))
        assertTrue(isRideable(mapOf("highway" to "track", "access" to "no", "bicycle" to "yes")))
        assertFalse(isRideable(mapOf("highway" to "path", "bicycle" to "no")))
        assertFalse(isRideable(mapOf("highway" to "service", "service" to "driveway")))
        assertFalse(isRideable(mapOf("building" to "yes")))
    }

    @Test
    fun fragmentsJoinIntoOneRingWhateverTheirDirection() {
        val a = GeoPoint(49.0, 2.0); val b = GeoPoint(49.0, 2.1); val c = GeoPoint(49.1, 2.1); val d = GeoPoint(49.1, 2.0)
        val rings = joinRings(listOf(listOf(a, b), listOf(c, b), listOf(c, d, a)))
        assertEquals(listOf(listOf(a, b, c, d)), rings)
    }

    @Test
    fun simplificationDropsNearlyStraightPointsOnly() {
        val square = PlaceGeometry.Polygon(
            listOf(
                GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.05), GeoPoint(49.00001, 2.06), // ~1 m off the edge
                GeoPoint(49.0, 2.1), GeoPoint(49.1, 2.1), GeoPoint(49.1, 2.0),
            ),
        )
        assertEquals(4, square.simplified(toleranceMeters = 15.0).exterior.size)
    }

    @Test
    fun placeCategoriesFromTags() {
        assertEquals(PlaceCategory.PARK, categoryOf(mapOf("leisure" to "park")))
        assertEquals(PlaceCategory.FOREST, categoryOf(mapOf("natural" to "wood")))
        assertEquals(PlaceCategory.LAKE, categoryOf(mapOf("natural" to "water", "water" to "lake")))
        assertEquals(PlaceCategory.LAKE, categoryOf(mapOf("natural" to "water")))
        assertNull(categoryOf(mapOf("natural" to "water", "water" to "river")), "the waterway line stands for it")
        assertEquals(PlaceCategory.RIVER, categoryOf(mapOf("waterway" to "river")))
        assertEquals(PlaceCategory.MONUMENT, categoryOf(mapOf("historic" to "memorial")))
        assertNull(categoryOf(mapOf("amenity" to "parking")))
    }

    @Test
    fun placesWithTheSameNameMergeAndOutsidersAreDropped() {
        val boundary = listOf(PlaceGeometry.Polygon(listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.1), GeoPoint(49.1, 2.1), GeoPoint(49.1, 2.0))))
        fun way(id: Int, tags: String, vararg points: Pair<Double, Double>) = Json.parseToJsonElement(
            """{"type":"way","id":$id,"tags":{$tags},"geometry":[${points.joinToString { "{\"lat\":${it.first},\"lon\":${it.second}}" }}]}""",
        ).jsonObject
        val river = """"waterway":"river","name":"L'Oise""""
        val elements = listOf(
            way(2, river, 49.05 to 2.05, 49.06 to 2.06),
            way(1, river, 49.06 to 2.06, 49.07 to 2.07),
            way(3, """"leisure":"park","name":"Loin"""", 50.0 to 2.0, 50.0 to 2.01, 50.01 to 2.01, 50.0 to 2.0),
            way(4, """"leisure":"park","name":"Sans fin"""", 49.05 to 2.05, 49.05 to 2.06, 49.06 to 2.06),
        )
        val places = placesOf(elements, boundary)
        assertEquals(1, places.size, "outside park and unclosed park dropped")
        val oise = places.single()
        assertEquals("way/1", oise.id.value)
        assertEquals(PlaceGeometry.MultiLineString(listOf(PlaceGeometry.LineString(listOf(GeoPoint(49.05, 2.05), GeoPoint(49.06, 2.06), GeoPoint(49.07, 2.07))))), oise.geometry)
    }
}
