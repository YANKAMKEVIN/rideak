package com.rideak.shared.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.CoreLocation.CLAccuracyAuthorization
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.NSObject

private class AuthorizationObserver : NSObject(), CLLocationManagerDelegateProtocol {
    val manager = CLLocationManager().also { it.delegate = this }
    var access by mutableStateOf(current())

    fun current(): LocationAccess = when (manager.authorizationStatus) {
        kCLAuthorizationStatusNotDetermined -> LocationAccess.NOT_DETERMINED
        kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways ->
            if (manager.accuracyAuthorization == CLAccuracyAuthorization.CLAccuracyAuthorizationReducedAccuracy) {
                LocationAccess.APPROXIMATE
            } else {
                LocationAccess.PRECISE
            }
        else -> LocationAccess.DENIED
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        access = current()
    }
}

@Composable
actual fun rememberLocationPermission(): LocationPermissionController {
    val observer = remember { AuthorizationObserver() }
    DisposableEffect(observer) { onDispose { observer.manager.delegate = null } }
    return LocationPermissionController(
        access = observer.access,
        // "While using the app" is enough: the location background mode keeps recording screen-off.
        request = { observer.manager.requestWhenInUseAuthorization() },
        openSettings = {
            NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
                UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null)
            }
        },
    )
}
