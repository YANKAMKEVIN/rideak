package com.rideak.shared.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RideakSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    /** Minimum touch target everywhere (accessibility). */
    val touchTarget: Dp = 48.dp,
    /** Minimum touch target in Ride mode: gloves, vibrations, quick glances. */
    val rideTouchTarget: Dp = 72.dp,
)

@Immutable
data class RideakShapes(
    val small: Shape = RoundedCornerShape(8.dp),
    val medium: Shape = RoundedCornerShape(16.dp),
    val large: Shape = RoundedCornerShape(24.dp),
    val pill: Shape = CircleShape,
)
