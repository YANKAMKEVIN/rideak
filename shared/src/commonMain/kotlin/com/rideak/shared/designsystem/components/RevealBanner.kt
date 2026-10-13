package com.rideak.shared.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.icons.RideakIcons

/**
 * End-of-ride reward: how many cells this ride revealed for the first time. A ride on known
 * ground is never shamed — it just says so and nudges toward an unknown street.
 * Nothing is shown for a ride that touched no cell.
 */
@Composable
fun RevealBanner(revealed: Int, traversed: Int, modifier: Modifier = Modifier) {
    if (traversed == 0) return
    val colors = RideakTheme.colors
    val accent = if (revealed > 0) colors.reward else colors.lightSurface
    val headline = when (revealed) {
        0 -> "Terrain connu"
        1 -> "+1 zone dévoilée !"
        else -> "+$revealed zones dévoilées !"
    }
    val detail = if (revealed > 0) "sur $traversed traversées · ta carte s'agrandit"
    else "$traversed zones revisitées · prends une rue inconnue la prochaine fois"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RideakTheme.shapes.medium)
            .border(1.dp, accent.copy(alpha = 0.5f), RideakTheme.shapes.medium)
            .padding(horizontal = RideakTheme.spacing.m, vertical = 14.dp)
            .clearAndSetSemantics { contentDescription = "$headline $detail" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(44.dp).background(accent.copy(alpha = 0.15f), RideakTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(RideakIcons.Leaf, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f)) {
            if (revealed > 0) {
                Text(
                    headline.uppercase(),
                    style = RideakTheme.typography.impact.copy(fontSize = 28.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
                    color = accent,
                )
            } else {
                Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}
