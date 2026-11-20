package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceAnchorTest {
    private fun place(geometry: PlaceGeometry) = Place(PlaceId("p"), "p", PlaceCategory.PARK, geometry)

    @Test
    fun pointIsItsOwnAnchor() {
        assertEquals(GeoPoint(49.0, 2.0), place(PlaceGeometry.Point(GeoPoint(49.0, 2.0))).anchor)
    }

    @Test
    fun lineIsAnchoredOnItsMiddleVertex() {
        val line = PlaceGeometry.LineString(listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.1), GeoPoint(49.0, 2.2)))
        assertEquals(GeoPoint(49.0, 2.1), place(line).anchor)
    }

    @Test
    fun polygonIsAnchoredInsideItsLargestPart() {
        val small = PlaceGeometry.Polygon(listOf(GeoPoint(48.0, 1.0), GeoPoint(48.0, 1.1), GeoPoint(48.1, 1.0)))
        val square = PlaceGeometry.Polygon(listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.2), GeoPoint(49.2, 2.2), GeoPoint(49.2, 2.0)))
        val anchor = place(PlaceGeometry.MultiPolygon(listOf(small, square))).anchor
        assertEquals(49.1, anchor.latitude, 1e-9)
        assertEquals(2.1, anchor.longitude, 1e-9)
    }
}
