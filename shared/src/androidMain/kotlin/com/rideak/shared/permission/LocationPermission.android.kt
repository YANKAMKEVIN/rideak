package com.rideak.shared.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

private val requested = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    // The ride notification (foreground service) is visible only with this on Android 13+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

@Composable
actual fun rememberLocationPermission(): LocationPermissionController {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    var access by remember { mutableStateOf(context.locationAccess(asked)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        access = context.locationAccess(asked = true)
    }
    LifecycleResumeEffect(asked) {
        access = context.locationAccess(asked)
        onPauseOrDispose {}
    }
    return LocationPermissionController(
        access = access,
        request = { launcher.launch(requested) },
        openSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )
}

private fun Context.granted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun Context.locationAccess(asked: Boolean) = when {
    granted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationAccess.PRECISE
    granted(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationAccess.APPROXIMATE
    asked -> LocationAccess.DENIED
    else -> LocationAccess.NOT_DETERMINED
}
