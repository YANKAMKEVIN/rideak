package com.rideak.shared.feature.places

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import com.rideak.exploration.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class DiscoveryDatesTest {
    // ~73 m per 0.001° of longitude at 49° N.
    private fun at(eastMeters: Double) = GeoPoint(49.0364, 2.07 + eastMeters / 73.0 * 0.001)
    private fun ride(toMeters: Int, start: Long) =
        listOf((0..toMeters step 20).mapIndexed { i, m -> TrackPoint(at(m.toDouble()), start + i * 4_000L) })
    private fun monument(id: String, eastMeters: Double) = Place(PlaceId(id), id, PlaceCategory.MONUMENT, PlaceGeometry.Point(at(eastMeters)))

    @Test
    fun eachPlaceIsDatedByTheFirstRideThatReachedIt() {
        val near = monument("near", 200.0)
        val far = monument("far", 1_200.0)
        val never = monument("never", 5_000.0)
        val dates = discoveryDates(
            listOf(near, far, never),
            listOf(1_000L to ride(400, 1_000L), 2_000L to ride(1_500, 2_000L), 3_000L to ride(1_500, 3_000L)),
        )
        assertEquals(mapOf(PlaceId("near") to 1_000L, PlaceId("far") to 2_000L), dates)
    }
}
