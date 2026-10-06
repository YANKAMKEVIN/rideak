package com.rideak.shared.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalRideakColors = staticCompositionLocalOf { RideakColors() }
private val LocalRideakTypography = staticCompositionLocalOf { RideakTypography() }
private val LocalRideakSpacing = staticCompositionLocalOf { RideakSpacing() }
private val LocalRideakShapes = staticCompositionLocalOf { RideakShapes() }

/** Dark-only for now: the RIDEAK art direction is built on Night. */
@Composable
fun RideakTheme(content: @Composable () -> Unit) {
    val colors = RideakColors()
    val typography = RideakTypography()
    CompositionLocalProvider(
        LocalRideakColors provides colors,
        LocalRideakTypography provides typography,
        LocalRideakSpacing provides RideakSpacing(),
        LocalRideakShapes provides RideakShapes(),
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = typography.ui,
            content = content,
        )
    }
}

object RideakTheme {
    val colors: RideakColors
        @Composable @ReadOnlyComposable get() = LocalRideakColors.current
    val typography: RideakTypography
        @Composable @ReadOnlyComposable get() = LocalRideakTypography.current
    val spacing: RideakSpacing
        @Composable @ReadOnlyComposable get() = LocalRideakSpacing.current
    val shapes: RideakShapes
        @Composable @ReadOnlyComposable get() = LocalRideakShapes.current
}
