package com.rideak.ride

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import kotlin.jvm.JvmInline

enum class RideStatus { RECORDING, PAUSED, FINISHED }

/** A ride as stored and listed. Live-only values (current speed) are not part of it. */
data class RideSummary(
    val id: RideId,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val status: RideStatus,
    /** Time spent recording, pauses excluded. */
    val activeDurationMillis: Long,
    val distanceMeters: Double,
    val movingMillis: Long,
    val maxSpeedMps: Double,
    val elevationGainMeters: Double,
    val spotCount: Int = 0,
) {
    val averageMovingSpeedMps: Double
        get() = if (movingMillis <= 0) 0.0 else distanceMeters / (movingMillis / 1000.0)
}

@JvmInline
value class SpotId(val value: String)

/**
 * A ❤️ dropped during a ride: "I like this spot". A user choice, unrelated to discovery.
 * May later be linked to a real place (OSM) near [position].
 */
data class Spot(
    val id: SpotId,
    val rideId: RideId,
    val position: GeoPoint,
    val createdAtEpochMillis: Long,
)
