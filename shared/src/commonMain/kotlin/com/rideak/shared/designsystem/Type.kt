package com.rideak.shared.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Two voices. Font files are not bundled yet (see docs/design-system.md): both families fall back
 * to the platform sans-serif and are swapped here, in one place, once Inter / Bangers (OFL) land.
 */
private val UiFontFamily = FontFamily.Default
private val DisplayFontFamily = FontFamily.Default

@Immutable
data class RideakTypography(
    /** UI voice: navigation, stats, settings, history. Always very legible. */
    val ui: Typography = Typography(),
    /** Manga voice, used sparingly: "NOUVEL ENDROIT !", "GO!", milestones. */
    val impact: TextStyle = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.01).em,
    ),
    /** Big ride values (speed, distance) readable at a glance on a handlebar. */
    val metric: TextStyle = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    /** Label of a primary action: large + bold, so white-on-red reaches the 3:1 large-text ratio. */
    val action: TextStyle = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.02.em,
    ),
)
