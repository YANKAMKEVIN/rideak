package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

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
    fun invalidZoomIsRejected() {
        assertFailsWith<IllegalArgumentException> { SlippyTileGrid(0) }
    }
}
