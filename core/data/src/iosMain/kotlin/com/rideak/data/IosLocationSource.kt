package com.rideak.data

import com.rideak.data.location.LocationSource
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import platform.CoreLocation.CLActivityTypeFitness
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLDistanceFilterNone
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/**
 * CoreLocation, best accuracy, fitness activity. Keeps updating in background thanks to the
 * `location` background mode declared in Info.plist (blue indicator shown to the user).
 */
class IosLocationSource : LocationSource {
    override fun locations(): Flow<TrackPoint> = callbackFlow {
        val delegate = LocationDelegate { locations -> locations.forEach { trySend(it.toTrackPoint()) } }
        val manager = CLLocationManager().apply {
            this.delegate = delegate
            desiredAccuracy = kCLLocationAccuracyBest
            distanceFilter = kCLDistanceFilterNone
            activityType = CLActivityTypeFitness
            pausesLocationUpdatesAutomatically = false
            allowsBackgroundLocationUpdates = true
            showsBackgroundLocationIndicator = true
            startUpdatingLocation()
        }
        awaitClose {
            manager.stopUpdatingLocation()
            manager.delegate = null
            delegate.hashCode() // keeps the (weakly referenced) delegate alive until closed
        }
    }.flowOn(Dispatchers.Main) // CLLocationManager needs a run loop: create it on the main thread
}

private class LocationDelegate(
    private val onLocations: (List<CLLocation>) -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        onLocations(didUpdateLocations.filterIsInstance<CLLocation>())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun CLLocation.toTrackPoint(): TrackPoint {
    val (latitude, longitude) = coordinate.useContents { latitude to longitude }
    return TrackPoint(
        position = GeoPoint(latitude, longitude),
        timestampEpochMillis = (timestamp.timeIntervalSince1970 * 1000).toLong(),
        horizontalAccuracyMeters = horizontalAccuracy.takeIf { it >= 0 }?.toFloat(),
        altitudeMeters = altitude.takeIf { verticalAccuracy > 0 },
        speedMetersPerSecond = speed.takeIf { it >= 0 }?.toFloat(),
    )
}
