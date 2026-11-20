package com.rideak.shared.feature.summary

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NearestPlaceTest {
    // ~73 m per 0.001° of longitude at 49° N.
    private fun at(eastMeters: Double) = GeoPoint(49.0364, 2.07 + eastMeters / 73.0 * 0.001)
    private fun place(name: String, eastMeters: Double) = Place(PlaceId(name), name, PlaceCategory.PARK, PlaceGeometry.Point(at(eastMeters)))

    @Test
    fun aSpotIsNamedAfterTheClosestPlaceNearby() {
        val places = listOf(place("Loin", 250.0), place("Parc", 100.0))
        assertEquals("Parc", nearestPlace(at(0.0), places)?.name)
        assertNull(nearestPlace(at(0.0), listOf(place("Trop loin", 1_000.0))))
    }
}
