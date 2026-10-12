package com.rideak.shared

import com.rideak.data.RideRepository
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import com.rideak.ride.Spot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory storage for UI tests. */
class FakeRideRepository : RideRepository {
    private val rides = MutableStateFlow<Map<RideId, RideSummary>>(emptyMap())
    private val points = mutableMapOf<RideId, MutableList<Pair<Int, TrackPoint>>>()
    private val spots = mutableListOf<Spot>()

    override suspend fun createRide(summary: RideSummary) { rides.value += summary.id to summary }
    override suspend fun saveSummary(summary: RideSummary) { rides.value += summary.id to summary }
    override suspend fun appendPoints(rideId: RideId, segment: Int, points: List<TrackPoint>) {
        this.points.getOrPut(rideId) { mutableListOf() } += points.map { segment to it }
    }
    override suspend fun addSpot(spot: Spot) { spots += spot }
    override suspend fun ride(rideId: RideId) = rides.value[rideId]
    override suspend fun unfinishedRide() = rides.value.values.firstOrNull { it.status != RideStatus.FINISHED }
    override suspend fun track(rideId: RideId) =
        points[rideId].orEmpty().groupBy({ it.first }, { it.second }).entries.sortedBy { it.key }.map { it.value }
    override suspend fun spots(rideId: RideId) = spots.filter { it.rideId == rideId }
    override fun observeFinishedRides(): Flow<List<RideSummary>> =
        rides.map { all -> all.values.filter { it.status == RideStatus.FINISHED } }
    override suspend fun finishedSegments(): List<List<TrackPoint>> = emptyList()
    override suspend fun finishedSpots(): List<Spot> = emptyList()
}
