package com.rideak.tools.regionpack

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PlaceGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
