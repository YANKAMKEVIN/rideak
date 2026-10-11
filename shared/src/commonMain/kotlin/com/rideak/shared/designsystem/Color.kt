package com.rideak.shared.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * RIDEAK palette v0.1. Measured contrasts (WCAG) are in docs/design-system.md.
 * Rule: text on Exploration Blue / Action Red is Night, or white only when large & bold
 * (white on red = 3.31:1, white on blue = 3.35:1 — fails AA for normal text).
 */
@Immutable
data class RideakColors(
    val exploration: Color = Color(0xFF238CFF),
    val nature: Color = Color(0xFF35D66F),
    val action: Color = Color(0xFFFF4B45),
    val reward: Color = Color(0xFFFFD43B),
    val night: Color = Color(0xFF071521),
    val surface: Color = Color(0xFF122A38),
    /** "Surface claire" of the board. */
    val surfaceRaised: Color = Color(0xFF1E2A38),
    val lightSurface: Color = Color(0xFFA7B3C2),
    val mangaWhite: Color = Color(0xFFF7F4EA),
) {
    // Semantic aliases: components use these, not raw palette names.
    val success: Color get() = nature
    val alert: Color get() = action
    val info: Color get() = exploration
    val neutral: Color get() = lightSurface
    val textPrimary: Color get() = mangaWhite
    val textSecondary: Color get() = lightSurface
    /** Text drawn on any saturated palette colour (blue, green, red, yellow). */
    val onAccent: Color get() = night
}

internal fun RideakColors.toMaterialScheme(): ColorScheme = darkColorScheme(
    primary = exploration,
    onPrimary = onAccent,
    secondary = nature,
    onSecondary = onAccent,
    tertiary = reward,
    onTertiary = onAccent,
    error = action,
    onError = onAccent,
    background = night,
    onBackground = textPrimary,
    surface = surface,
    onSurface = textPrimary,
    surfaceVariant = surfaceRaised,
    onSurfaceVariant = textSecondary,
    surfaceContainer = surface,
    outline = lightSurface,
)
