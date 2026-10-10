package com.rideak.ride

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

/** Synthetic traces around Cergy for replay tests. */
internal object Traces {
    val start = GeoPoint(49.0364, 2.0761)
    private const val METERS_PER_DEGREE = 111_195.0

    fun offset(origin: GeoPoint, northMeters: Double, eastMeters: Double) = GeoPoint(
        origin.latitude + northMeters / METERS_PER_DEGREE,
        origin.longitude + eastMeters / (METERS_PER_DEGREE * cos(origin.latitude * PI / 180)),
    )

    /** Straight ride east at [speedMps], one fix per second. */
    fun straight(
        seconds: Int,
        speedMps: Double,
        from: GeoPoint = start,
        startMillis: Long = 0,
        accuracy: Float = 5f,
        altitude: (Int) -> Double? = { null },
        reportSpeed: Boolean = true,
    ): List<TrackPoint> = (0..seconds).map { s ->
        TrackPoint(
            position = offset(from, 0.0, speedMps * s),
            timestampEpochMillis = startMillis + s * 1000L,
            horizontalAccuracyMeters = accuracy,
            altitudeMeters = altitude(s),
            speedMetersPerSecond = if (reportSpeed) speedMps.toFloat() else null,
        )
    }

    /** Rider standing still while the fix wanders up to [jitterMeters] around [at]. */
    fun stationary(seconds: Int, jitterMeters: Double, at: GeoPoint = start, accuracy: Float = 8f, seed: Int = 42): List<TrackPoint> {
        val random = Random(seed)
        return (0..seconds).map { s ->
            TrackPoint(
                position = offset(at, random.nextDouble(-jitterMeters, jitterMeters), random.nextDouble(-jitterMeters, jitterMeters)),
                timestampEpochMillis = s * 1000L,
                horizontalAccuracyMeters = accuracy,
                speedMetersPerSecond = random.nextDouble(0.0, 0.4).toFloat(),
            )
        }
    }
}
