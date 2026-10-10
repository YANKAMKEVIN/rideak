package com.rideak.shared.permission

import androidx.compose.runtime.Composable

enum class LocationAccess { NOT_DETERMINED, DENIED, APPROXIMATE, PRECISE }

class LocationPermissionController(
    val access: LocationAccess,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/** Location permission as seen by the ride screen. Re-evaluated when the app comes back to the foreground. */
@Composable
expect fun rememberLocationPermission(): LocationPermissionController
