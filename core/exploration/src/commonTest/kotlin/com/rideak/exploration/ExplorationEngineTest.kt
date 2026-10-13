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

    // ~73 m per 0.001° of longitude at 49° N; a zoom-17 cell is ~0.00275° wide.
    private fun east(meters: Double) = meters / 73.0 * 0.001

    @Test
    fun interpolationExploresTheCellsBetweenTwoDistantSamples() {
        val interpolating = ExplorationEngine(
            SlippyTileGrid(17),
            interpolation = Interpolation(maxGapMillis = 60_000, maxGapMeters = 1_000.0, stepMeters = 10.0),
        )
        // ~600 m east in 40 s: three cells in between are crossed without any sample in them.
        val track = listOf(point(49.0364, 2.0761, 0), point(49.0364, 2.0761 + east(600.0), 40_000))
        assertEquals(2, engine.cellsFor(track).size)
        assertEquals(
            (66291..66294).map { CellId("tile17/$it/44992") }.toSet(),
            interpolating.cellsFor(track),
        )
    }

    @Test
    fun signalGapIsNotBridged() {
        val interpolating = ExplorationEngine(
            SlippyTileGrid(17),
            interpolation = Interpolation(maxGapMillis = 60_000, maxGapMeters = 1_000.0, stepMeters = 10.0),
        )
        val tooLong = listOf(point(49.0364, 2.0761, 0), point(49.0364, 2.0761 + east(600.0), 5 * 60_000))
        val tooFar = listOf(point(49.0364, 2.0761, 0), point(49.0364, 2.0761 + east(2_000.0), 30_000))
        assertEquals(2, interpolating.cellsFor(tooLong).size)
        assertEquals(2, interpolating.cellsFor(tooFar).size)
    }

    @Test
    fun segmentsAreNeverJoined() {
        val interpolating = ExplorationEngine(
            SlippyTileGrid(17),
            interpolation = Interpolation(maxGapMillis = 60_000, maxGapMeters = 1_000.0, stepMeters = 10.0),
        )
        val before = listOf(point(49.0364, 2.0761, 0))
        val after = listOf(point(49.0364, 2.0761 + east(600.0), 10_000))
        assertEquals(2, interpolating.cellsForSegments(listOf(before, after)).size)
    }

    @Test
    fun driftWhileStoppedExploresNothingAround() {
        // Stopped right next to a cell border (~3 m west of it), fixes wandering 10 m around.
        val border = 2.076416015625 // west edge of tile 66292
        val stopped = (0 until 60).map { i ->
            val jitter = if (i % 2 == 0) east(-13.0) else east(7.0)
            point(49.0364, border + east(-3.0) + jitter, i * 1_000L)
        }
        assertEquals(2, engine.cellsFor(stopped).size, "raw drift crosses the border")
        val filtered = ExplorationEngine(SlippyTileGrid(17), listOf(DriftFilter(minDisplacementMeters = 25.0)))
        assertEquals(1, filtered.cellsFor(stopped).size)
    }

    @Test
    fun driftFilterKeepsRealMovement() {
        val ride = (0 until 30).map { i -> point(49.0364, 2.0761 + east(i * 5.0), i * 1_000L) } // 5 m/s
        val kept = DriftFilter(minDisplacementMeters = 15.0).apply(ride)
        assertTrue(kept.size in 7..10, "one sample every ~15 m, got ${kept.size}")
        assertEquals(engine.cellsFor(ride), engine.cellsFor(kept))
    }

    @Test
    fun sustainedMotorisedSpeedIsRemoved() {
        val filter = MotorizedSpeedFilter(maxSpeedMetersPerSecond = 12.5, minDurationMillis = 60_000)
        val bike = (0 until 10).map { i -> point(49.0364, 2.0745 + east(i * 5.0), i * 1_000L) }
        // Then 2 minutes of train at 30 m/s.
        val train = (1..120).map { i -> point(49.0364, 2.0745 + east(45.0 + i * 30.0), (9 + i) * 1_000L) }
        val kept = filter.apply(bike + train)
        assertEquals(bike.dropLast(1), kept)
    }

    @Test
    fun shortFastDescentIsKept() {
        val filter = MotorizedSpeedFilter(maxSpeedMetersPerSecond = 12.5, minDurationMillis = 60_000)
        // 20 s at 15 m/s (54 km/h) between slow riding.
        var x = 0.0
        val track = (0 until 60).map { i ->
            x += if (i in 20 until 40) 15.0 else 5.0
            point(49.0364, 2.0761 + east(x), i * 1_000L)
        }
        assertEquals(track, filter.apply(track))
    }

    @Test
    fun standardEngineIgnoresATrainRide() {
        val bike = (0 until 10).map { i -> point(49.0364, 2.0745 + east(i * 5.0), i * 1_000L) }
        val train = (1..120).map { i -> point(49.0364, 2.0745 + east(45.0 + i * 30.0), (9 + i) * 1_000L) }
        assertEquals(1, ExplorationEngine.standard(SlippyTileGrid(17)).cellsFor(bike + train).size)
    }

    @Test
    fun revealedCellsExcludeEverythingRiddenBefore() {
        val a = point(49.0364, 2.0745, 0)
        val b = point(49.0364, 2.0775, 30_000) // next cell east
        val c = point(49.0364, 2.0800, 60_000) // and the one after
        val earlier = listOf(listOf(a, b))
        val ride = listOf(listOf(a, b, c))
        assertEquals(setOf(engine.cellsFor(listOf(c)).single()), engine.revealedBy(ride, earlier))
        assertEquals(engine.cellsForSegments(ride), engine.revealedBy(ride, before = emptyList()))
        assertTrue(engine.revealedBy(earlier, before = ride).isEmpty())
    }
}
