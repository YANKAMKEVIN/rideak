package com.rideak.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import com.rideak.exploration.ExplorationProgress
import com.rideak.shared.designsystem.RideakTheme
import kotlin.math.roundToInt

/** Region progression. The percentage is always written: progress is never conveyed by colour alone. */
@Composable
fun ExplorationProgressBar(
    regionName: String,
    progress: ExplorationProgress,
    modifier: Modifier = Modifier,
) {
    val fraction = progress.fraction.toFloat().coerceIn(0f, 1f)
    val percentText = "${(fraction * 100).roundToInt()} % exploré"
    Column(
        modifier = modifier
            .clip(RideakTheme.shapes.medium)
            .background(RideakTheme.colors.surface)
            .padding(RideakTheme.spacing.m)
            .clearAndSetSemantics {
                contentDescription = "$regionName, $percentText"
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(regionName, style = MaterialTheme.typography.titleMedium, color = RideakTheme.colors.textPrimary)
            Text(percentText, style = MaterialTheme.typography.labelLarge, color = RideakTheme.colors.textSecondary)
        }
        Box(
            Modifier.fillMaxWidth().height(10.dp).clip(RideakTheme.shapes.pill)
                .background(RideakTheme.colors.surfaceRaised),
        ) {
            Box(
                Modifier.fillMaxWidth(fraction).height(10.dp).clip(RideakTheme.shapes.pill)
                    .background(RideakTheme.colors.nature),
            )
        }
    }
}
