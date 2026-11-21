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

    @Test
    fun curiositiesThatAreBusinessesOrPlayAreasAreLeftOut() {
        // Kept: the kind of curiosities the Cergy pack shows (Axe Majeur, Les Douze Colonnes, the park sculptures).
        assertEquals(PlaceCategory.LANDMARK, categoryOf(mapOf("tourism" to "attraction", "name" to "Axe Majeur")))
        assertEquals(PlaceCategory.LANDMARK, categoryOf(mapOf("tourism" to "artwork", "artwork_type" to "sculpture", "name" to "Les Douze Colonnes")))
        assertEquals(PlaceCategory.LANDMARK, categoryOf(mapOf("tourism" to "artwork", "amenity" to "fountain", "name" to "Fontaine")))
        assertEquals(PlaceCategory.LANDMARK, categoryOf(mapOf("tourism" to "attraction", "fee" to "no", "name" to "Belvédère")))
        // Left out: how OSM usually tags a ride, a play park or a shop marked as an attraction.
        assertNull(categoryOf(mapOf("tourism" to "attraction", "attraction" to "amusement_ride", "name" to "Aeroscoot")))
        assertNull(categoryOf(mapOf("tourism" to "attraction", "leisure" to "playground", "name" to "Baby Park")))
        assertNull(categoryOf(mapOf("tourism" to "attraction", "fee" to "yes", "name" to "Accrobranche")))
        assertNull(categoryOf(mapOf("tourism" to "attraction", "phone" to "+33 1 00 00 00 00", "name" to "Location de pédalos")))
        assertNull(categoryOf(mapOf("tourism" to "attraction", "shop" to "gift", "name" to "Boutique")))
        assertNull(categoryOf(mapOf("tourism" to "attraction", "amenity" to "restaurant", "name" to "Guinguette")))
        assertNull(categoryOf(mapOf("tourism" to "attraction", "sport" to "climbing", "name" to "Mur d'escalade")))
        // The rule only applies to curiosities: a park with a phone number is still a park.
        assertEquals(PlaceCategory.PARK, categoryOf(mapOf("leisure" to "park", "phone" to "+33 1 00 00 00 00")))
        assertEquals(PlaceCategory.MONUMENT, categoryOf(mapOf("historic" to "castle", "fee" to "yes")))
    }

    @Test
    fun excludedElementsNeverReachThePack() {
        val boundary = listOf(PlaceGeometry.Polygon(listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.1), GeoPoint(49.1, 2.1), GeoPoint(49.1, 2.0))))
        fun node(id: Long, name: String, lat: Double, lon: Double) = Json.parseToJsonElement(
            """{"type":"node","id":$id,"lat":$lat,"lon":$lon,"tags":{"tourism":"attraction","name":"$name"}}""",
        ).jsonObject
        // The real ids and positions of the first Cergy pack, with bare tags that isBusiness cannot catch.
        val places = placesOf(
            listOf(
                node(6873617685, "Aeroscoot", 49.0272, 2.051784),
                node(11611881631, "Baby Park", 49.025726, 2.052004),
                node(262554703, "Les Douze Colonnes", 49.042205, 2.03783),
            ),
            boundary,
        )
        assertEquals(listOf("Les Douze Colonnes"), places.map { it.name })
    }
}
