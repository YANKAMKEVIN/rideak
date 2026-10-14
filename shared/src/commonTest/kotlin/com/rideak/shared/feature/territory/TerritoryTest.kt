package com.rideak.shared.feature.territory

import com.rideak.exploration.CellId
import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.RegionId
import com.rideak.shared.designsystem.components.formatPercent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerritoryTest {
    private fun region(name: String, vararg cells: String) = ExplorationRegion(
        RegionId(name), name,
        listOf(PlaceGeometry.Polygon(listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0), GeoPoint(1.0, 1.0)))),
        "tile17", cells.mapTo(LinkedHashSet()) { CellId(it) },
    )

    private val cergy = region("Cergy", "a", "b", "c", "d")
    private val pontoise = region("Pontoise", "e", "f")

    @Test
    fun noTerritoryUntilARegionIsExplored() {
        assertNull(territoryOf(listOf(cergy, pontoise), setOf(CellId("elsewhere"))))
    }

    @Test
    fun theMostExploredRegionIsPutForward() {
        val explored = setOf(CellId("a"), CellId("e"), CellId("f"))
        assertEquals(TerritoryProgress("Pontoise", 2, 2), territoryOf(listOf(cergy, pontoise), explored))
    }

    @Test
    fun percentIsFlooredAndNeverHidesAStart() {
        assertEquals("0 %", formatPercent(0f))
        assertEquals("< 1 %", formatPercent(1f / 391))
        assertEquals("12 %", formatPercent(0.129f))
        assertEquals("99 %", formatPercent(390f / 391))
        assertEquals("100 %", formatPercent(1f))
    }
}
