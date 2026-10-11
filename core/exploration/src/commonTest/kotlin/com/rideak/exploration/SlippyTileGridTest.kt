package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlippyTileGridTest {
    @Test
    fun originAtZoomOneIsBottomRightQuadrant() {
        assertEquals(CellId("tile1/1/1"), SlippyTileGrid(1).cellAt(GeoPoint(-0.0001, 0.0001)))
    }

    @Test
    fun cellIdsFromDifferentGridsNeverCollide() {
        val p = GeoPoint(49.0364, 2.0761)
        assertNotEquals(SlippyTileGrid(16).cellAt(p), SlippyTileGrid(17).cellAt(p))
    }

    @Test
    fun polesAndAntimeridianStayInsideTheGrid() {
        val grid = SlippyTileGrid(3)
        assertEquals(CellId("tile3/7/0"), grid.cellAt(GeoPoint(90.0, 180.0)))
        assertEquals(CellId("tile3/0/7"), grid.cellAt(GeoPoint(-90.0, -180.0)))
    }

    @Test
    fun cellBoundsContainTheirPoint() {
        val grid = SlippyTileGrid(17)
        val p = GeoPoint(49.0364, 2.0761)
        val b = assertNotNull(grid.boundsOf(grid.cellAt(p)))
        assertTrue(p.latitude in b.south..b.north && p.longitude in b.west..b.east)
        assertTrue((b.east - b.west) * 111_195 * kotlin.math.cos(49.0364 * kotlin.math.PI / 180) in 190.0..210.0)
    }

    @Test
    fun boundsOfAForeignCellIsNull() {
        assertNull(SlippyTileGrid(17).boundsOf(CellId("tile16/1/1")))
        assertNull(SlippyTileGrid(17).boundsOf(CellId("h3/abc")))
    }

    @Test
    fun invalidZoomIsRejected() {
        assertFailsWith<IllegalArgumentException> { SlippyTileGrid(0) }
    }
}
