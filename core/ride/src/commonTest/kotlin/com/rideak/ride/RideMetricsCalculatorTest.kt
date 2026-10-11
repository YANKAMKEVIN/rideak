package com.rideak.ride

import com.rideak.exploration.TrackPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RideMetricsCalculatorTest {
    private val calculator = RideMetricsCalculator()

    private fun metrics(vararg segments: List<TrackPoint>) = calculator.compute(segments.toList()).metrics

    private fun assertNear(expected: Double, actual: Double, tolerance: Double) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected ± $tolerance, was $actual")

    @Test
    fun emptyRideHasZeroMetrics() {
        assertEquals(RideMetrics(), metrics())
    }

    @Test
    fun straightRideMeasuresDistanceAndMovingTime() {
        // 200 s at 5 m/s (18 km/h) = 1 000 m.
        val m = metrics(Traces.straight(seconds = 200, speedMps = 5.0))
        assertNear(1000.0, m.distanceMeters, 10.0)
        assertEquals(200_000, m.movingMillis)
        assertNear(5.0, m.currentSpeedMps, 0.01)
        assertNear(5.0, m.maxSpeedMps, 0.01)
    }

    @Test
    fun speedIsDerivedFromPositionsWhenTheOsDoesNotReportIt() {
        val m = metrics(Traces.straight(seconds = 60, speedMps = 6.0, reportSpeed = false))
        assertNear(6.0, m.currentSpeedMps, 0.1)
        assertNear(360.0, m.distanceMeters, 6.0)
    }

    @Test
    fun standingStillDoesNotAccumulateDistanceOrMovingTime() {
        // 5 minutes at a red light with the fix wandering ±6 m.
        val m = metrics(Traces.stationary(seconds = 300, jitterMeters = 6.0))
        assertTrue(m.distanceMeters < 25.0, "drift accumulated ${m.distanceMeters} m")
        assertEquals(0, m.movingMillis)
    }

    @Test
    fun inaccurateFixesAreIgnored() {
        val good = Traces.straight(seconds = 10, speedMps = 5.0)
        val bad = TrackPoint(Traces.offset(Traces.start, 500.0, 0.0), 10_500, horizontalAccuracyMeters = 120f)
        val m = metrics(good + bad)
        assertNear(50.0, m.distanceMeters, 5.0)
    }

    @Test
    fun aSingleGpsJumpIsRejected() {
        val track = Traces.straight(seconds = 20, speedMps = 5.0).toMutableList()
        // One fix 300 m away for a single second, then back on track.
        track[10] = track[10].copy(position = Traces.offset(track[10].position, 300.0, 0.0), speedMetersPerSecond = null)
        val m = metrics(track)
        assertNear(100.0, m.distanceMeters, 10.0)
        assertTrue(m.maxSpeedMps < 6.0, "spike leaked into max speed: ${m.maxSpeedMps}")
    }

    @Test
    fun aConsistentNewPositionIsEventuallyTrusted() {
        val before = Traces.straight(seconds = 5, speedMps = 5.0)
        // Every following fix is 2 km away (first fix was wrong): trusted after 3 glitches, jump not counted.
        val after = Traces.straight(seconds = 20, speedMps = 5.0, from = Traces.offset(Traces.start, 2000.0, 0.0), startMillis = 6_000)
        val m = metrics(before + after)
        assertTrue(m.distanceMeters in 90.0..130.0, "distance was ${m.distanceMeters}")
    }

    @Test
    fun pausedGapIsNeverCounted() {
        val first = Traces.straight(seconds = 60, speedMps = 5.0)
        // Resumes 10 minutes later, 1 km further (e.g. walked with the bike while paused).
        val second = Traces.straight(
            seconds = 60, speedMps = 5.0,
            from = Traces.offset(Traces.start, 0.0, 1300.0), startMillis = 660_000,
        )
        val m = metrics(first, second)
        assertNear(600.0, m.distanceMeters, 12.0)
        assertEquals(120_000, m.movingMillis)
    }

    @Test
    fun signalLossDoesNotCountAsMovingTime() {
        val first = Traces.straight(seconds = 30, speedMps = 5.0)
        val afterTunnel = Traces.straight(seconds = 30, speedMps = 5.0, from = first.last().position, startMillis = 90_000)
        val m = metrics(first + afterTunnel)
        assertEquals(60_000, m.movingMillis) // the 60 s gap is excluded
    }

    @Test
    fun altitudeNoiseIsNotCountedAsClimbing() {
        val noisy = Traces.straight(seconds = 120, speedMps = 5.0, altitude = { s -> 50.0 + if (s % 2 == 0) 2.0 else -2.0 })
        assertEquals(0.0, metrics(noisy).elevationGainMeters)
    }

    @Test
    fun realClimbIsCounted() {
        // Climbs 60 m over 2 minutes, then descends: only the climb counts.
        val climb = Traces.straight(seconds = 240, speedMps = 4.0, altitude = { s -> 30.0 + 60.0 * (if (s <= 120) s else 240 - s) / 120 })
        assertNear(60.0, metrics(climb).elevationGainMeters, 5.0)
        assertNear(60.0, metrics(climb).elevationLossMeters, 5.0)
    }

    @Test
    fun liveAndReplayedMetricsAreIdentical() {
        val track = Traces.straight(seconds = 100, speedMps = 5.0) + Traces.stationary(seconds = 0, jitterMeters = 1.0)
        val live = track.fold(RideMetricsState(), calculator::add)
        assertEquals(live.metrics, calculator.compute(listOf(track)).metrics)
    }

    @Test
    fun averageMovingSpeedExcludesStops() {
        val summary = RideSummary(
            id = com.rideak.exploration.RideId("r"), startedAtEpochMillis = 0, endedAtEpochMillis = 600_000,
            status = RideStatus.FINISHED, activeDurationMillis = 600_000, distanceMeters = 3000.0,
            movingMillis = 400_000, maxSpeedMps = 9.0, elevationGainMeters = 0.0,
        )
        assertNear(7.5, summary.averageMovingSpeedMps, 0.001)
    }
}
