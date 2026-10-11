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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.icons.RideakIcons

/** Exploration milestones: the bar always shows progress toward the next one. */
private val milestones = listOf(10, 25, 50, 100, 250, 500, 1_000, 2_500, 5_000, 10_000)

@Composable
fun TerritoryCard(exploredCells: Int, modifier: Modifier = Modifier) {
    val colors = RideakTheme.colors
    val next = milestones.firstOrNull { it > exploredCells } ?: (exploredCells + 1)
    val previous = milestones.lastOrNull { it <= exploredCells } ?: 0
    val fraction = ((exploredCells - previous).toFloat() / (next - previous)).coerceIn(0f, 1f)
    val subtitle = if (exploredCells == 0) "Ta première sortie dévoilera ta carte" else "$exploredCells zones explorées"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RideakTheme.shapes.medium)
            .border(1.dp, colors.lightSurface.copy(alpha = 0.25f), RideakTheme.shapes.medium)
            .padding(horizontal = RideakTheme.spacing.m, vertical = 14.dp)
            .clearAndSetSemantics {
                contentDescription = "Mon territoire, $subtitle, prochain palier $next zones"
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(colors.nature.copy(alpha = 0.15f), RideakTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(RideakIcons.Leaf, contentDescription = null, tint = colors.nature, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Mon territoire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
            Text("$exploredCells / $next", style = MaterialTheme.typography.labelLarge, color = colors.nature)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(colors.surfaceRaised, RideakTheme.shapes.pill)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).background(colors.nature, RideakTheme.shapes.pill))
        }
    }
}

