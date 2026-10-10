package com.rideak.data.location

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Debug tool: replays a ~3.8 km loop around Cergy at ~22 km/h with a gentle climb, one fix per second,
 * so the whole ride flow can be tested at a desk, on device or simulator.
 */
class SimulatedLocationSource(
    private val clock: () -> Long,
    private val intervalMillis: Long = 1_000,
    private val speedMps: Double = 6.0,
) : LocationSource {
    private val center = GeoPoint(49.0364, 2.0761)
    private val radiusMeters = 600.0

    override fun locations(): Flow<TrackPoint> = flow {
        val circumference = 2 * PI * radiusMeters
        var travelled = 0.0
        while (true) {
            val angle = travelled / circumference * 2 * PI
            val north = radiusMeters * sin(angle)
            val east = radiusMeters * cos(angle)
            emit(
                TrackPoint(
                    position = GeoPoint(
                        center.latitude + north / 111_195.0,
                        center.longitude + east / (111_195.0 * cos(center.latitude * PI / 180)),
                    ),
                    timestampEpochMillis = clock(),
                    horizontalAccuracyMeters = 5f,
                    altitudeMeters = 40.0 + 25.0 * (1 + sin(angle)),
                    speedMetersPerSecond = speedMps.toFloat(),
                ),
            )
            travelled += speedMps * intervalMillis / 1000.0
            delay(intervalMillis)
        }
    }
}
