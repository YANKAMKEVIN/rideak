package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiveDiscoveryTest {
    // ~73 m per 0.001° of longitude at 49° N.
    private val origin = GeoPoint(49.0364, 2.0700)
    private fun at(eastMeters: Double) = GeoPoint(origin.latitude, origin.longitude + eastMeters / 73.0 * 0.001)

    /** Riding due east from [fromMeters] to [toMeters], one fix every 20 m and 4 s. */
    private fun ride(fromMeters: Int, toMeters: Int, startMillis: Long = 0) =
        (fromMeters..toMeters step 20).mapIndexed { i, m -> TrackPoint(at(m.toDouble()), startMillis + i * 4_000L) }

    private val monument = Place(PlaceId("node/1"), "Tour", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(900.0)))

    @Test
    fun reportsOnlyWhatIsNewSinceTheLastUpdate() {
        val live = LiveDiscovery(before = emptyList(), places = listOf(monument))
        val track = ride(0, 1_400)

        val first = live.update(listOf(track.take(20)))
        assertTrue(first.newCells > 0)
        assertTrue(first.newPlaces.isEmpty(), "the monument is 900 m away")

        val second = live.update(listOf(track))
        assertEquals(listOf(monument), second.newPlaces)
        assertEquals(second.revealedCells - first.revealedCells, second.newCells)
        assertEquals(1, second.discoveredPlaces)

        val again = live.update(listOf(track))
        assertEquals(0, again.newCells)
        assertTrue(again.newPlaces.isEmpty())
    }

    @Test
    fun groundAndPlacesKnownFromEarlierRidesAreNotAnnounced() {
        val earlier = ride(0, 1_400)
        val live = LiveDiscovery(before = listOf(earlier), places = listOf(monument))

        val update = live.update(listOf(ride(0, 1_400, startMillis = 10_000_000)))

        assertEquals(0, update.revealedCells)
        assertTrue(update.newPlaces.isEmpty())
    }

    @Test
    fun matchesTheEndOfRideCount() {
        val engine = ExplorationEngine.standard()
        val earlier = ride(0, 600)
        val today = ride(0, 1_400, startMillis = 10_000_000)
        val live = LiveDiscovery(before = listOf(earlier), places = emptyList(), engine = engine)

        assertEquals(engine.revealedBy(listOf(today), listOf(earlier)).size, live.update(listOf(today)).revealedCells)
    }
}
