package com.rideak.shared.permission

import androidx.compose.runtime.Composable

enum class LocationAccess { NOT_DETERMINED, DENIED, APPROXIMATE, PRECISE }

class LocationPermissionController(
    val access: LocationAccess,
    /** Device-wide location switch: when off, no app gets any fix whatever its permission. */
    val servicesEnabled: Boolean,
    val request: () -> Unit,
    val openSettings: () -> Unit,
    val openLocationSettings: () -> Unit,
)

/** Location permission as seen by the ride screen. Re-evaluated when the app comes back to the foreground. */
@Composable
expect fun rememberLocationPermission(): LocationPermissionController
