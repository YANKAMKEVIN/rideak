package com.rideak.exploration

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** WGS84 position. Provider-neutral: never an Android `Location` or a map SDK type. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "longitude out of range: $longitude" }
    }
}

private const val EARTH_RADIUS_METERS = 6_371_008.8

/** Great-circle (haversine) distance. Accurate to well under 1 % at cycling scales. */
fun GeoPoint.distanceTo(other: GeoPoint): Double {
    val lat1 = latitude.toRadians()
    val lat2 = other.latitude.toRadians()
    val dLat = lat2 - lat1
    val dLon = (other.longitude - longitude).toRadians()
    val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

internal fun Double.toRadians(): Double = this * kotlin.math.PI / 180.0
