package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegionTest {
    private val grid = SlippyTileGrid(17)

    // ~1.1 km × ~0.7 km square around Cergy-Préfecture, with a ~100 m "lake" hole in the middle.
    private val lake = listOf(GeoPoint(49.0355, 2.0755), GeoPoint(49.0355, 2.0770), GeoPoint(49.0365, 2.0770), GeoPoint(49.0365, 2.0755))
    private val square = PlaceGeometry.Polygon(
        listOf(GeoPoint(49.030, 2.070), GeoPoint(49.030, 2.085), GeoPoint(49.040, 2.085), GeoPoint(49.040, 2.070)),
        holes = listOf(lake),
    )

    @Test
    fun polygonContainsRespectsHoles() {
        assertTrue(GeoPoint(49.032, 2.072) in square)
        assertFalse(GeoPoint(49.036, 2.076) in square, "inside the lake")
        assertFalse(GeoPoint(49.045, 2.072) in square, "north of the boundary")
        assertFalse(GeoPoint(49.035, 2.090) in square, "east of the boundary")
    }

    @Test
    fun cellsAlongALineHaveNoGap() {
        // ~730 m due east crosses 4 adjacent tiles; samples every 10 m cannot skip one.
        val cells = grid.cellsAlong(listOf(GeoPoint(49.0364, 2.0745), GeoPoint(49.0364, 2.0845)), stepMeters = 10.0)
        val xs = cells.map { it.value.split('/')[1].toInt() }.sorted()
        assertEquals((xs.first()..xs.last()).toList(), xs)
        assertEquals(1, cells.map { it.value.split('/')[2] }.toSet().size)
    }

    @Test
    fun rideableCellsOnlyCountTheStretchesInsideTheBoundary() {
        // A road running from inside the square to 2 km outside of it.
        val road = listOf(GeoPoint(49.032, 2.072), GeoPoint(49.032, 2.110))
        val all = grid.cellsAlong(road, 10.0)
        val inside = rideableCells(listOf(square), listOf(road), grid)
        assertTrue(inside.isNotEmpty())
        assertTrue(inside.size < all.size)
        val eastEdge = grid.cellAt(GeoPoint(49.032, 2.085)).value.split('/')[1].toInt()
        assertTrue(inside.all { it.value.split('/')[1].toInt() <= eastEdge })
    }

    @Test
    fun packRoundTrips() {
        val region = ExplorationRegion(
            id = RegionId("osm-relation-1"),
            name = "Cergy",
            boundary = listOf(square),
            gridId = grid.id,
            rideableCells = setOf(CellId("tile17/66291/44992"), CellId("tile17/66292/44992")),
        )
        val text = RegionPackCodec.encode(region, "© OpenStreetMap contributors (ODbL)")
        assertTrue("cells 66291/44992 66292/44992" in text)
        val pack = RegionPackCodec.decode(text)
        assertEquals(region, pack.region)
        assertEquals("© OpenStreetMap contributors (ODbL)", pack.attribution)
    }

    @Test
    fun packRejectsForeignText() {
        assertFailsWith<IllegalArgumentException> { RegionPackCodec.decode("{\"type\":\"FeatureCollection\"}") }
    }
}
