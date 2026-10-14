package com.rideak.shared.feature.progress

import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideak.exploration.Badge
import com.rideak.exploration.Level
import com.rideak.exploration.PlayerStats
import com.rideak.shared.designsystem.RideakTheme
import org.jetbrains.compose.resources.painterResource

/** Level, title and XP toward the next level. */
@Composable
fun LevelCard(level: Level, modifier: Modifier = Modifier) {
    val c = RideakTheme.colors
    val fraction = level.fractionToNext.coerceIn(0f, 1f)
    Column(
        modifier.fillMaxWidth()
            .background(c.surface, RideakTheme.shapes.medium)
            .border(1.dp, c.reward.copy(alpha = 0.4f), RideakTheme.shapes.medium)
            .padding(RideakTheme.spacing.m)
            .clearAndSetSemantics {
                contentDescription = "Niveau ${level.number}, ${level.title}, ${level.xp} XP, prochain niveau à ${level.nextLevelXp} XP"
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).background(c.reward, RideakTheme.shapes.pill), contentAlignment = Alignment.Center) {
                Text("${level.number}", style = RideakTheme.typography.impact.copy(fontSize = 30.sp), color = c.onAccent)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    level.title.uppercase(),
                    style = RideakTheme.typography.impact.copy(fontSize = 26.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
                    color = c.reward,
                )
                Text("Niveau ${level.number} · ${level.xp} XP", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(c.surfaceRaised, RideakTheme.shapes.pill)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).background(c.reward, RideakTheme.shapes.pill))
        }
        Text(
            "Encore ${level.nextLevelXp - level.xp} XP avant le niveau ${level.number + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
        )
    }
}

/** "Mes badges": every badge, unlocked in colour, locked in grey with its progress. */
@Composable
fun BadgeGrid(stats: PlayerStats, modifier: Modifier = Modifier) {
    val c = RideakTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
        val unlocked = Badge.entries.count { it.unlocked(stats) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mes badges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            Text("$unlocked / ${Badge.entries.size}", style = MaterialTheme.typography.labelLarge, color = c.reward)
        }
        Badge.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
                row.forEach { badge -> BadgeTile(badge, stats, Modifier.weight(1f)) }
            }
        }
    }
}

private val greyscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun BadgeTile(badge: Badge, stats: PlayerStats, modifier: Modifier) {
    val c = RideakTheme.colors
    val unlocked = badge.unlocked(stats)
    val progress = badge.progress(stats)
    Column(
        modifier.background(c.surface, RideakTheme.shapes.medium).padding(vertical = 10.dp, horizontal = 6.dp)
            .clearAndSetSemantics {
                contentDescription = if (unlocked) "Badge ${badge.title}, débloqué" else "Badge ${badge.title}, verrouillé, ${badge.description}, $progress sur ${badge.goal}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painterResource(badge.art),
            contentDescription = null,
            colorFilter = if (unlocked) null else greyscale,
            modifier = Modifier.size(72.dp).alpha(if (unlocked) 1f else 0.45f),
        )
        Text(badge.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (unlocked) c.textPrimary else c.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
        Text(
            if (unlocked) "Débloqué" else "$progress / ${badge.goal}",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) c.nature else c.textSecondary,
        )
    }
}

/** End-of-ride rewards: XP earned and badges unlocked by this ride. */
@Composable
fun RideRewards(xp: Int, badges: List<Badge>, modifier: Modifier = Modifier) {
    if (xp <= 0 && badges.isEmpty()) return
    val c = RideakTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (xp > 0) {
            Text(
                "+$xp XP",
                style = RideakTheme.typography.impact.copy(fontSize = 26.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
                color = c.reward,
            )
        }
        badges.forEach { badge ->
            Row(
                Modifier.fillMaxWidth()
                    .background(c.surface, RideakTheme.shapes.medium)
                    .border(1.dp, c.reward.copy(alpha = 0.6f), RideakTheme.shapes.medium)
                    .padding(RideakTheme.spacing.s)
                    .clearAndSetSemantics { contentDescription = "Badge débloqué : ${badge.title}" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(painterResource(badge.art), contentDescription = null, modifier = Modifier.size(64.dp))
                Column {
                    Text(
                        "BADGE DÉBLOQUÉ !",
                        style = RideakTheme.typography.impact.copy(fontSize = 22.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
                        color = c.reward,
                    )
                    Text(badge.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                }
            }
        }
    }
}
