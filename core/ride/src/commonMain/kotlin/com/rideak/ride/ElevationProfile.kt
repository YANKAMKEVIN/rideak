package com.rideak.ride

import com.rideak.exploration.TrackPoint
import com.rideak.exploration.distanceTo

data class ProfilePoint(val distanceMeters: Double, val altitudeMeters: Double)

/**
 * Altitude along the ride, for the summary chart. Distance accumulates within each segment only
 * (pauses never bridged). Downsampled to at most [maxPoints] so drawing stays cheap on long rides.
 */
fun elevationProfile(segments: List<List<TrackPoint>>, maxPoints: Int = 200): List<ProfilePoint> {
    val points = mutableListOf<ProfilePoint>()
    var distance = 0.0
    for (segment in segments) {
        var previous: TrackPoint? = null
        for (point in segment) {
            previous?.let { distance += it.position.distanceTo(point.position) }
            previous = point
            point.altitudeMeters?.let { points += ProfilePoint(distance, it) }
        }
    }
    if (points.size <= maxPoints) return points
    val step = points.size.toDouble() / maxPoints
    return List(maxPoints) { i -> points[(i * step).toInt()] } + points.last()
}
