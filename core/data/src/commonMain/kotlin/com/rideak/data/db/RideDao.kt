package com.rideak.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Insert
    suspend fun insertRide(ride: RideEntity)

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Insert
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Insert
    suspend fun insertSpot(spot: SpotEntity)

    /** Points and spots follow (ON DELETE CASCADE). A ride being recorded is kept. */
    @Query("DELETE FROM rides WHERE status = 'FINISHED'")
    suspend fun deleteFinishedRides()

    @Query("SELECT r.*, (SELECT COUNT(*) FROM spots s WHERE s.rideId = r.id) AS spotCount FROM rides r WHERE r.id = :id")
    suspend fun ride(id: String): RideWithSpotCount?

    @Query(
        "SELECT r.*, (SELECT COUNT(*) FROM spots s WHERE s.rideId = r.id) AS spotCount FROM rides r " +
            "WHERE r.status != 'FINISHED' ORDER BY r.startedAt DESC LIMIT 1",
    )
    suspend fun unfinishedRide(): RideWithSpotCount?

    @Query(
        "SELECT r.*, (SELECT COUNT(*) FROM spots s WHERE s.rideId = r.id) AS spotCount FROM rides r " +
            "WHERE r.status = 'FINISHED' ORDER BY r.startedAt DESC",
    )
    fun observeFinishedRides(): Flow<List<RideWithSpotCount>>

    @Query("SELECT * FROM track_points WHERE rideId = :rideId ORDER BY segment, timestamp, id")
    suspend fun points(rideId: String): List<TrackPointEntity>

    @Query(
        "SELECT tp.* FROM track_points tp JOIN rides r ON r.id = tp.rideId " +
            "WHERE r.status = 'FINISHED' ORDER BY tp.rideId, tp.segment, tp.timestamp, tp.id",
    )
    suspend fun finishedPoints(): List<TrackPointEntity>

    @Query(
        "SELECT tp.* FROM track_points tp JOIN rides r ON r.id = tp.rideId " +
            "WHERE r.status = 'FINISHED' AND r.startedAt < :startedAt ORDER BY tp.rideId, tp.segment, tp.timestamp, tp.id",
    )
    suspend fun finishedPointsBefore(startedAt: Long): List<TrackPointEntity>

    @Query("SELECT s.* FROM spots s JOIN rides r ON r.id = s.rideId WHERE r.status = 'FINISHED' ORDER BY s.createdAt")
    suspend fun finishedSpots(): List<SpotEntity>

    @Query("SELECT * FROM spots WHERE rideId = :rideId ORDER BY createdAt")
    suspend fun spots(rideId: String): List<SpotEntity>
}
