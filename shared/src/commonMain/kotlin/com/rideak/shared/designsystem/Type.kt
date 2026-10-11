package com.rideak.shared.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.rideak.shared.resources.Res
import com.rideak.shared.resources.bangers_regular
import com.rideak.shared.resources.inter_bold
import com.rideak.shared.resources.inter_extrabold
import com.rideak.shared.resources.inter_medium
import com.rideak.shared.resources.inter_regular
import com.rideak.shared.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/**
 * Two voices (fonts under SIL OFL, licences in docs/licenses):
 * - Inter: UI, stats, settings — always legible;
 * - Bangers: manga impact, used sparingly ("23 KM/H", "NOUVEL ENDROIT !", milestones).
 */
@Immutable
data class RideakTypography(
    val ui: Typography,
    /** Manga voice. */
    val impact: TextStyle,
    /** Big ride values readable at a glance on a handlebar. */
    val metric: TextStyle,
    /** Label of a primary action: large + bold, so white-on-red reaches the 3:1 large-text ratio. */
    val action: TextStyle,
)

@Composable
internal fun rideakTypography(): RideakTypography {
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
        Font(Res.font.inter_extrabold, FontWeight.ExtraBold),
    )
    val bangers = FontFamily(Font(Res.font.bangers_regular, FontWeight.Normal))
    val base = Typography()
    fun TextStyle.inter() = copy(fontFamily = inter)
    return RideakTypography(
        ui = Typography(
            displayLarge = base.displayLarge.inter(),
            displayMedium = base.displayMedium.inter(),
            displaySmall = base.displaySmall.inter(),
            headlineLarge = base.headlineLarge.inter(),
            headlineMedium = base.headlineMedium.inter(),
            headlineSmall = base.headlineSmall.inter(),
            titleLarge = base.titleLarge.inter(),
            titleMedium = base.titleMedium.inter(),
            titleSmall = base.titleSmall.inter(),
            bodyLarge = base.bodyLarge.inter(),
            bodyMedium = base.bodyMedium.inter(),
            bodySmall = base.bodySmall.inter(),
            labelLarge = base.labelLarge.inter(),
            labelMedium = base.labelMedium.inter(),
            labelSmall = base.labelSmall.inter(),
        ),
        impact = TextStyle(
            fontFamily = bangers,
            fontSize = 44.sp,
            lineHeight = 46.sp,
            letterSpacing = 0.02.em,
        ),
        metric = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),
        action = TextStyle(
            fontFamily = inter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.02.em,
        ),
    )
}
