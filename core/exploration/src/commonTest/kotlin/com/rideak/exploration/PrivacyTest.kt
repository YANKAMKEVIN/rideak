package com.rideak.exploration

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PrivacyTest {
    private val home = GeoPoint(49.0364, 2.0761)
    private val zone = PrivacyZone("z", "Domicile", home, radiusMeters = 300.0)

    @Test
    fun zoneAroundASecretNeverCentresOnItButAlwaysCoversIt() {
        repeat(200) { seed ->
            val z = PrivacyZone.around("z", "Domicile", home, radiusMeters = 400.0, random = Random(seed))
            val shift = z.center.distanceTo(home)
            assertTrue(home in z, "seed $seed")
            assertTrue(shift >= 0.15 * 400 * 0.99, "seed $seed: centre only $shift m away")
            assertTrue(z.radiusMeters - shift >= 0.5 * 400, "seed $seed: home too close to the edge")
        }
        val centers = (0 until 20).map { PrivacyZone.around("z", "D", home, 400.0, Random(it)).center }.toSet()
        assertEquals(20, centers.size, "the shift is random")
    }

    @Test
    fun linesAreCutAtTheZone() {
        // From 1 km west of home, through home, to 1 km east, one point every 100 m.
        val line = (-10..10).map { home.offset(northMeters = 0.0, eastMeters = it * 100.0) }
        val visible = PrivacyMask(listOf(zone)).lines(listOf(line))
        assertEquals(2, visible.size, "the crossing splits the ride in two")
        assertTrue(visible.flatten().none { it in zone })
        assertEquals(line.count { it !in zone }, visible.sumOf { it.size })
    }

    @Test
    fun noZoneMasksNothing() {
        val line = listOf(home, home.offset(0.0, 100.0))
        assertEquals(listOf(line), PrivacyMask(emptyList()).lines(listOf(line)))
    }

    @Test
    fun cellsTouchingAZoneAreHidden() {
        val grid = SlippyTileGrid(17)
        val homeCell = grid.cellAt(home)
        val farCell = grid.cellAt(home.offset(0.0, 2_000.0))
        val edgeCell = grid.cellAt(home.offset(0.0, 380.0)) // centre outside the zone, cell overlapping it
        assertEquals(listOf(farCell), PrivacyMask(listOf(zone)).cells(listOf(homeCell, edgeCell, farCell), grid))
    }

    @Test
    fun frequentStartsAreSuggestedOnce() {
        val work = home.offset(northMeters = 3_000.0, eastMeters = 0.0)
        val endpoints = listOf(
            home, home.offset(20.0, 30.0), home.offset(-40.0, 10.0), // home, 3 times
            work, work.offset(10.0, 10.0), // work, twice
            home.offset(5_000.0, 5_000.0), // a one-off
        )
        val suggestions = suggestPrivacySpots(endpoints, existing = emptyList())
        assertEquals(2, suggestions.size)
        assertEquals(listOf(3, 2), suggestions.map { it.rides })
        assertTrue(suggestions[0].position.distanceTo(home) < 50)
        assertTrue(suggestions[1].position.distanceTo(work) < 50)
        val afterProtectingHome = suggestPrivacySpots(endpoints, existing = listOf(zone))
        assertEquals(1, afterProtectingHome.size)
        assertNotEquals(suggestions[0], afterProtectingHome[0])
        assertFalse(afterProtectingHome[0].position in zone)
    }
}
