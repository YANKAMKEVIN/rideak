package com.rideak.shared.feature.rides

import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class RideListTest {
    // ~73 m per 0.001° of longitude at 49° N.
    private fun track(toMeters: Int, start: Long) =
        listOf((0..toMeters step 20).mapIndexed { i, m -> TrackPoint(GeoPoint(49.0364, 2.07 + m / 73.0 * 0.001), start + i * 4_000L) })

    private fun ride(id: String, start: Long, meters: Double) =
        RideSummary(RideId(id), start, start + 1_000, RideStatus.FINISHED, 1_000, meters, 1_000, 5.0, 0.0)

    // Noon UTC: 11 October and 2 November 2026.
    private val october = 1_791_720_000_000L
    private val november = 1_793_620_800_000L

    @Test
    fun ridesAreGroupedByMonthWithTheirZonesRevealedFirst() {
        val engine = ExplorationEngine.standard()
        val items = rideListItems(
            listOf(
                ride("again", november, 1_000.0) to track(600, november),
                ride("first", october, 2_000.0) to track(600, october),
                ride("further", october + 86_400_000, 3_000.0) to track(1_400, october + 86_400_000),
            ),
            engine,
        )
        assertEquals(listOf("again", "further", "first"), items.map { it.ride.id.value })
        assertEquals(0, items[0].revealedCells, "same streets as the first ride")
        val firstCells = engine.cellsForSegments(track(600, 0)).size
        assertEquals(firstCells, items[2].revealedCells)
        assertEquals(engine.cellsForSegments(track(1_400, 0)).size - firstCells, items[1].revealedCells)

        val months = groupByMonth(items)
        assertEquals(listOf("Novembre 2026", "Octobre 2026"), months.map { it.title })
        assertEquals(5_000.0, months[1].totalMeters)
    }
}
