package com.rideak.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rideak.data.location.LocationSource
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Fused provider, high accuracy, ~1 fix per second while a ride is recording. */
class FusedLocationSource(private val context: Context) : LocationSource {

    // Permission is checked by the UI before a ride starts; if it was revoked meanwhile,
    // the SecurityException closes the flow and the recorder shows "searching".
    @SuppressLint("MissingPermission")
    override fun locations(): Flow<TrackPoint> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toTrackPoint()) }
            }
        }
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e)
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }
}

private fun Location.toTrackPoint() = TrackPoint(
    position = GeoPoint(latitude, longitude),
    timestampEpochMillis = time,
    horizontalAccuracyMeters = if (hasAccuracy()) accuracy else null,
    altitudeMeters = if (hasAltitude()) altitude else null,
    speedMetersPerSecond = if (hasSpeed()) speed else null,
)
