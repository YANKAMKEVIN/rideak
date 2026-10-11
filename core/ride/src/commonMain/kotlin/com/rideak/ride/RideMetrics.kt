package com.rideak.ride

import com.rideak.exploration.TrackPoint
import com.rideak.exploration.distanceTo
import kotlin.math.max
import kotlin.math.min

/**
 * Thresholds of the ride filters. Defaults are starting points to be validated with real rides —
 * every value is a parameter, never a hardcoded business rule.
 */
data class RideFilterConfig(
    /** Fixes reported less accurate than this are ignored. */
    val maxAccuracyMeters: Float = 25f,
    /** Distance only grows once the rider has moved at least max(this, fix accuracy): absorbs drift when stopped. */
    val minStepMeters: Double = 5.0,
    /** A jump faster than this is a GPS glitch (not a speed rule: 25 m/s = 90 km/h). */
    val maxPlausibleSpeedMps: Double = 25.0,
    /** After this many consecutive glitches, the new position is trusted (the first fix was the wrong one). */
    val maxConsecutiveGlitches: Int = 3,
    /** Below this speed the rider is considered stopped (0.8 m/s ≈ 3 km/h). */
    val minMovingSpeedMps: Double = 0.8,
    /** Longer gaps between fixes (signal loss) do not count as moving time. */
    val maxMovingGapMillis: Long = 30_000,
    /** GPS altitude is noisy: climbs smaller than this are ignored. */
    val elevationHysteresisMeters: Double = 5.0,
    /** Exponential smoothing factor of the displayed speed (0..1, higher = more reactive). */
    val speedSmoothing: Double = 0.4,
)

data class RideMetrics(
    val distanceMeters: Double = 0.0,
    val movingMillis: Long = 0,
    val currentSpeedMps: Double = 0.0,
    val maxSpeedMps: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val elevationLossMeters: Double = 0.0,
)

/** Calculator state: metrics plus what is needed to process the next fix. Immutable. */
data class RideMetricsState(
    val metrics: RideMetrics = RideMetrics(),
    /** Last fix received, accepted or not (GPS status, ❤️ position). */
    val lastFix: TrackPoint? = null,
    internal val previous: TrackPoint? = null,
    internal val anchor: TrackPoint? = null,
    internal val elevationReference: Double? = null,
    internal val consecutiveGlitches: Int = 0,
)

/**
 * Turns raw TrackPoints into ride metrics, one fix at a time. Pure: replaying the stored points of a
 * ride ([compute]) gives exactly what was shown live, so history can be recomputed when filters change.
 */
class RideMetricsCalculator(private val config: RideFilterConfig = RideFilterConfig()) {

    fun add(state: RideMetricsState, point: TrackPoint): RideMetricsState {
        val received = state.copy(lastFix = point)
        val accuracy = point.horizontalAccuracyMeters
        if (accuracy != null && accuracy > config.maxAccuracyMeters) return received

        val previous = state.previous ?: return received.copy(
            previous = point,
            anchor = point,
            elevationReference = point.altitudeMeters,
        )
        val dtMillis = point.timestampEpochMillis - previous.timestampEpochMillis
        if (dtMillis <= 0) return received

        val impliedSpeed = previous.position.distanceTo(point.position) / (dtMillis / 1000.0)
        if (impliedSpeed > config.maxPlausibleSpeedMps) {
            val glitches = state.consecutiveGlitches + 1
            return if (glitches < config.maxConsecutiveGlitches) {
                received.copy(consecutiveGlitches = glitches)
            } else {
                // The new position is consistent: restart from it without counting the jump.
                received.copy(previous = point, anchor = point, consecutiveGlitches = 0)
            }
        }

        val metrics = state.metrics
        val anchor = state.anchor ?: previous
        val step = anchor.position.distanceTo(point.position)
        // Without an OS (Doppler) speed, use the net displacement since the anchor, not since the previous
        // fix: drift jumps back and forth around a stopped rider, so its net speed stays near zero.
        val rawSpeed = point.speedMetersPerSecond?.toDouble()
            ?: (point.timestampEpochMillis - anchor.timestampEpochMillis).takeIf { it > 0 }?.let { step / (it / 1000.0) }
            ?: 0.0
        val smoothed = config.speedSmoothing * rawSpeed + (1 - config.speedSmoothing) * metrics.currentSpeedMps

        val moving = rawSpeed >= config.minMovingSpeedMps
        val threshold = max(config.minStepMeters, min(accuracy?.toDouble() ?: 0.0, config.maxAccuracyMeters.toDouble()))
        // Distance only grows while actually riding: a stopped rider never accumulates drift.
        val advanced = moving && step >= threshold

        var gain = metrics.elevationGainMeters
        var loss = metrics.elevationLossMeters
        var reference = state.elevationReference
        point.altitudeMeters?.let { altitude ->
            val ref = reference
            when {
                ref == null -> reference = altitude
                altitude - ref >= config.elevationHysteresisMeters -> { gain += altitude - ref; reference = altitude }
                ref - altitude >= config.elevationHysteresisMeters -> { loss += ref - altitude; reference = altitude }
            }
        }

        return received.copy(
            metrics = metrics.copy(
                distanceMeters = metrics.distanceMeters + if (advanced) step else 0.0,
                movingMillis = metrics.movingMillis + if (moving && dtMillis <= config.maxMovingGapMillis) dtMillis else 0,
                currentSpeedMps = smoothed,
                maxSpeedMps = max(metrics.maxSpeedMps, smoothed),
                elevationGainMeters = gain,
                elevationLossMeters = loss,
            ),
            previous = point,
            anchor = if (advanced) point else anchor,
            elevationReference = reference,
            consecutiveGlitches = 0,
        )
    }

    /** After a pause: the next fix starts fresh, so the paused gap is never counted. */
    fun newSegment(state: RideMetricsState): RideMetricsState = state.copy(
        metrics = state.metrics.copy(currentSpeedMps = 0.0),
        previous = null,
        anchor = null,
        elevationReference = null,
        consecutiveGlitches = 0,
    )

    /** Replays a stored ride, one list of points per recording segment. */
    fun compute(segments: List<List<TrackPoint>>): RideMetricsState =
        segments.fold(RideMetricsState()) { state, segment ->
            segment.fold(newSegment(state), ::add)
        }
}
