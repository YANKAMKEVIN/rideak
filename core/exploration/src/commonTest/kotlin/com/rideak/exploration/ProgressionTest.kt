package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressionTest {
    @Test
    fun xpRewardsExplorationNotSpeed() {
        val stats = PlayerStats(rides = 2, distanceMeters = 900.0, exploredCells = 8)
        assertEquals(8 * 10 + 2 * 20 + 0 * 5, Progression.xp(stats))
        val withPlaces = stats.copy(discoveredPlaces = mapOf(PlaceCategory.PARK to 1, PlaceCategory.RIVER to 1))
        assertEquals(Progression.xp(stats) + 100, Progression.xp(withPlaces))
    }

    @Test
    fun levelsGrowAndStartAtNovice() {
        assertEquals(Level(1, "Novice", 0, 0, 100), Progression.level(0))
        assertEquals(1, Progression.level(99).number)
        assertEquals(Level(2, "Éclaireur", 120, 100, 300), Progression.level(120))
        assertEquals(0.1f, Progression.level(120).fractionToNext)
        assertEquals(3, Progression.level(300).number)
        assertEquals(5, Progression.level(1_000).number)
        assertEquals("Légende", Progression.level(1_000_000).title, "titles stop at the last one")
    }

    @Test
    fun badgesFollowStats() {
        val newcomer = PlayerStats()
        assertTrue(Badge.entries.none { it.unlocked(newcomer) })
        val stats = PlayerStats(rides = 3, distanceMeters = 12_400.0, exploredCells = 40, exploredRegions = 1)
        assertTrue(Badge.FIRST_RIDE.unlocked(stats))
        assertTrue(Badge.TEN_KM.unlocked(stats))
        assertFalse(Badge.HUNDRED_KM.unlocked(stats))
        assertEquals(12, Badge.HUNDRED_KM.progress(stats))
        assertEquals(40, Badge.EXPLORER.progress(stats))
        assertEquals(1, Badge.FIVE_CITIES.progress(stats))
        assertEquals(10, Badge.TEN_KM.progress(stats), "progress is capped at the goal")
    }

    @Test
    fun aRideUnlocksOnlyWhatItCrossed() {
        val before = PlayerStats(rides = 1, distanceMeters = 8_000.0)
        val after = PlayerStats(rides = 2, distanceMeters = 14_000.0, discoveredPlaces = mapOf(PlaceCategory.PARK to 1))
        assertEquals(listOf(Badge.TEN_KM, Badge.FIRST_PARK), Badge.newlyUnlocked(before, after))
        assertTrue(Badge.newlyUnlocked(after, after).isEmpty())
    }
}
