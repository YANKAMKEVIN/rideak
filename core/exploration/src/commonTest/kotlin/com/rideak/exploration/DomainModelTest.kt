package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DomainModelTest {
    private val square = PlaceGeometry.Polygon(
        listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.1), GeoPoint(49.1, 2.1), GeoPoint(49.1, 2.0)),
    )

    @Test
    fun regionProgressOnlyCountsRideableCells() {
        val region = ExplorationRegion(
            id = RegionId("cergy"),
            name = "Cergy",
            boundary = listOf(square),
            gridId = "tile17",
            rideableCells = setOf(CellId("a"), CellId("b"), CellId("c"), CellId("d")),
        )
        // "lake" is visited but not rideable: it must not inflate progression.
        val progress = region.progress(setOf(CellId("a"), CellId("lake")))
        assertEquals(ExplorationProgress(1, 4), progress)
        assertEquals(0.25, progress.fraction)
    }

    @Test
    fun emptyRegionHasZeroProgress() {
        assertEquals(0.0, ExplorationProgress(0, 0).fraction)
    }

    @Test
    fun geometriesRejectDegenerateShapes() {
        assertFailsWith<IllegalArgumentException> { PlaceGeometry.LineString(listOf(GeoPoint(0.0, 0.0))) }
        assertFailsWith<IllegalArgumentException> {
            PlaceGeometry.Polygon(listOf(GeoPoint(0.0, 0.0), GeoPoint(1.0, 1.0)))
        }
        assertFailsWith<IllegalArgumentException> { GeoPoint(91.0, 0.0) }
    }

    @Test
    fun privacyZoneContainsNearbyPointsOnly() {
        val home = PrivacyZone("home", "Maison", GeoPoint(49.0364, 2.0761), radiusMeters = 300.0)
        assertTrue(GeoPoint(49.0380, 2.0761) in home) // ~178 m north
        assertFalse(GeoPoint(49.0400, 2.0761) in home) // ~400 m north
    }

    @Test
    fun haversineDistanceIsAccurate() {
        // 0.001° of latitude ≈ 111.2 m
        val d = GeoPoint(49.0, 2.0).distanceTo(GeoPoint(49.001, 2.0))
        assertTrue(d in 110.0..112.5, "distance was $d")
    }
}
