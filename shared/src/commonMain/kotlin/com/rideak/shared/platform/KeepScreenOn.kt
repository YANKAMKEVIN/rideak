package com.rideak.shared.platform

import androidx.compose.runtime.Composable

/** Keeps the screen awake while [enabled] and this call is in the composition; released otherwise. */
@Composable
expect fun KeepScreenOn(enabled: Boolean)
