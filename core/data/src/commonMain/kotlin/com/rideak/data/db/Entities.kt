package com.rideak.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,
    val activeDurationMs: Long,
    val distanceM: Double,
    val movingMs: Long,
    val maxSpeedMps: Double,
    val elevationGainM: Double,
)

/** Raw GPS samples: the source of truth, kept unfiltered so everything can be recomputed. */
@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(RideEntity::class, ["id"], ["rideId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("rideId")],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rideId: String,
    /** Recording segment: incremented at each resume, so pauses are never bridged. */
    val segment: Int,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
)

@Entity(
    tableName = "spots",
    foreignKeys = [ForeignKey(RideEntity::class, ["id"], ["rideId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("rideId")],
)
data class SpotEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long,
)

data class RideWithSpotCount(
    @Embedded val ride: RideEntity,
    val spotCount: Int,
)
