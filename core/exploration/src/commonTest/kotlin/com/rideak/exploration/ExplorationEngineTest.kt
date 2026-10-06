package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExplorationEngineTest {
    private val engine = ExplorationEngine(SlippyTileGrid(zoom = 17))

    private fun point(lat: Double, lon: Double, t: Long = 0, accuracy: Float? = null) =
        TrackPoint(GeoPoint(lat, lon), timestampEpochMillis = t, horizontalAccuracyMeters = accuracy)

    @Test
    fun emptyTrackExploresNothing() {
        assertTrue(engine.cellsFor(emptyList()).isEmpty())
    }

    @Test
    fun singlePointExploresOneCell() {
        assertEquals(setOf(CellId("tile17/66291/44992")), engine.cellsFor(listOf(point(49.0364, 2.0761))))
    }

    @Test
    fun pointsInSameCellExploreOneCell() {
        val track = listOf(point(49.0364, 2.0761, 0), point(49.0370, 2.0761, 1_000), point(49.0366, 2.0763, 2_000))
        assertEquals(1, engine.cellsFor(track).size)
    }

    @Test
    fun movingAcrossCellsExploresEachCell() {
        val track = listOf(point(49.0364, 2.0761, 0), point(49.0364, 2.0775, 30_000))
        assertEquals(
            setOf(CellId("tile17/66291/44992"), CellId("tile17/66292/44992")),
            engine.cellsFor(track),
        )
    }

    @Test
    fun revisitingACellProducesNoDuplicate() {
        val a = point(49.0364, 2.0761, 0)
        val b = point(49.0364, 2.0775, 30_000)
        val track = listOf(a, b, a.copy(timestampEpochMillis = 60_000), b.copy(timestampEpochMillis = 90_000))
        assertEquals(2, engine.cellsFor(track).size)
    }

    @Test
    fun filtersRunBeforeCellsAreComputed() {
        val filtered = ExplorationEngine(SlippyTileGrid(17), listOf(AccuracyFilter(maxAccuracyMeters = 30f)))
        val track = listOf(
            point(49.0364, 2.0761, 0, accuracy = 8f),
            point(49.0364, 2.0900, 1_000, accuracy = 250f), // bad fix far away: must not explore
        )
        assertEquals(setOf(CellId("tile17/66291/44992")), filtered.cellsFor(track))
    }
}
