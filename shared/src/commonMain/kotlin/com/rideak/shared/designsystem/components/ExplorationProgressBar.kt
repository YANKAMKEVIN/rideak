package com.rideak.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rideak.exploration.ExplorationProgress
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.icons.RideakIcons
import kotlin.math.roundToInt

/** Region progression card. The percentage is always written: progress is never conveyed by colour alone. */
@Composable
fun ExplorationProgressBar(
    regionName: String,
    progress: ExplorationProgress,
    modifier: Modifier = Modifier,
) {
    val colors = RideakTheme.colors
    val fraction = progress.fraction.toFloat().coerceIn(0f, 1f)
    val percentText = "${(fraction * 100).roundToInt()} % exploré"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RideakTheme.shapes.medium)
            .background(colors.surface)
            .border(1.dp, colors.lightSurface.copy(alpha = 0.25f), RideakTheme.shapes.medium)
            .padding(RideakTheme.spacing.m)
            .clearAndSetSemantics {
                contentDescription = "$regionName, $percentText"
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).clip(RideakTheme.shapes.pill).background(colors.nature.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(RideakIcons.Leaf, contentDescription = null, tint = colors.nature, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(
                    regionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                Text(percentText, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RideakTheme.shapes.pill).background(colors.surfaceRaised)) {
            Box(
                Modifier.fillMaxWidth(fraction).height(10.dp).clip(RideakTheme.shapes.pill).background(colors.nature),
            )
        }
    }
}
