package com.rideak.data

import com.rideak.data.db.RideDao
import com.rideak.data.db.RideEntity
import com.rideak.data.db.RideWithSpotCount
import com.rideak.data.db.SpotEntity
import com.rideak.data.db.TrackPointEntity
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import com.rideak.ride.Spot
import com.rideak.ride.SpotId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Storage of rides. An interface so the recorder can be tested with an in-memory database or a fake. */
interface RideRepository {
    suspend fun createRide(summary: RideSummary)
    suspend fun saveSummary(summary: RideSummary)
    suspend fun appendPoints(rideId: RideId, segment: Int, points: List<TrackPoint>)
    suspend fun addSpot(spot: Spot)
    suspend fun ride(rideId: RideId): RideSummary?
    suspend fun unfinishedRide(): RideSummary?
    /** Stored points of a ride, one list per recording segment. */
    suspend fun track(rideId: RideId): List<List<TrackPoint>>
    suspend fun spots(rideId: RideId): List<Spot>
    fun observeFinishedRides(): Flow<List<RideSummary>>
    /** Every recording segment of every finished ride, as separate lines. */
    suspend fun finishedSegments(): List<List<TrackPoint>>
    /** Same as [finishedSegments], limited to rides started before [startedAtEpochMillis]. */
    suspend fun finishedSegmentsBefore(startedAtEpochMillis: Long): List<List<TrackPoint>>
    suspend fun finishedSpots(): List<Spot>
    /** First and last stored position of every finished ride: where rides start and end. */
    suspend fun finishedEndpoints(): List<GeoPoint>
    /** Right to erasure: every finished ride with its points and spots. */
    suspend fun deleteFinishedRides()
}

class RoomRideRepository(private val dao: RideDao) : RideRepository {
    override suspend fun createRide(summary: RideSummary) = dao.insertRide(summary.toEntity())

    override suspend fun saveSummary(summary: RideSummary) = dao.updateRide(summary.toEntity())

    override suspend fun appendPoints(rideId: RideId, segment: Int, points: List<TrackPoint>) {
        if (points.isEmpty()) return
        dao.insertPoints(
            points.map {
                TrackPointEntity(
                    rideId = rideId.value,
                    segment = segment,
                    timestamp = it.timestampEpochMillis,
                    latitude = it.position.latitude,
                    longitude = it.position.longitude,
                    accuracy = it.horizontalAccuracyMeters,
                    altitude = it.altitudeMeters,
                    speed = it.speedMetersPerSecond,
                )
            },
        )
    }

    override suspend fun addSpot(spot: Spot) = dao.insertSpot(
        SpotEntity(spot.id.value, spot.rideId.value, spot.position.latitude, spot.position.longitude, spot.createdAtEpochMillis),
    )

    override suspend fun ride(rideId: RideId) = dao.ride(rideId.value)?.toSummary()

    override suspend fun unfinishedRide() = dao.unfinishedRide()?.toSummary()

    override suspend fun track(rideId: RideId): List<List<TrackPoint>> =
        dao.points(rideId.value)
            .groupBy { it.segment }
            .entries
            .sortedBy { it.key }
            .map { (_, segment) ->
                segment.map { it.toTrackPoint() }
            }

    override suspend fun spots(rideId: RideId) = dao.spots(rideId.value).map {
        Spot(SpotId(it.id), RideId(it.rideId), GeoPoint(it.latitude, it.longitude), it.createdAt)
    }

    override fun observeFinishedRides(): Flow<List<RideSummary>> =
        dao.observeFinishedRides().map { rows -> rows.map { it.toSummary() } }

    override suspend fun finishedSegments() = dao.finishedPoints().toSegments()

    override suspend fun finishedSegmentsBefore(startedAtEpochMillis: Long) =
        dao.finishedPointsBefore(startedAtEpochMillis).toSegments()

    override suspend fun deleteFinishedRides() = dao.deleteFinishedRides()

    override suspend fun finishedEndpoints(): List<GeoPoint> =
        dao.finishedPoints().groupBy { it.rideId }.values.flatMap { points ->
            listOf(points.first(), points.last()).map { GeoPoint(it.latitude, it.longitude) }
        }

    override suspend fun finishedSpots() = dao.finishedSpots().map {
        Spot(SpotId(it.id), RideId(it.rideId), GeoPoint(it.latitude, it.longitude), it.createdAt)
    }
}

private fun List<TrackPointEntity>.toSegments(): List<List<TrackPoint>> =
    groupBy { it.rideId to it.segment }.values.map { segment -> segment.map { it.toTrackPoint() } }

private fun TrackPointEntity.toTrackPoint() =
    TrackPoint(GeoPoint(latitude, longitude), timestamp, accuracy, altitude, speed)

private fun RideSummary.toEntity() = RideEntity(
    id = id.value,
    startedAt = startedAtEpochMillis,
    endedAt = endedAtEpochMillis,
    status = status.name,
    activeDurationMs = activeDurationMillis,
    distanceM = distanceMeters,
    movingMs = movingMillis,
    maxSpeedMps = maxSpeedMps,
    elevationGainM = elevationGainMeters,
)

private fun RideWithSpotCount.toSummary() = RideSummary(
    id = RideId(ride.id),
    startedAtEpochMillis = ride.startedAt,
    endedAtEpochMillis = ride.endedAt,
    status = RideStatus.valueOf(ride.status),
    activeDurationMillis = ride.activeDurationMs,
    distanceMeters = ride.distanceM,
    movingMillis = ride.movingMs,
    maxSpeedMps = ride.maxSpeedMps,
    elevationGainMeters = ride.elevationGainM,
    spotCount = spotCount,
)
