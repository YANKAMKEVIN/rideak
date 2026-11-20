package com.rideak.shared.feature.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rideak.exploration.Badge
import com.rideak.exploration.Level
import com.rideak.exploration.PlayerStats
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.format.formatDay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

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
fun BadgeGrid(stats: PlayerStats, modifier: Modifier = Modifier, unlockedAt: Map<Badge, Long> = emptyMap()) {
    val c = RideakTheme.colors
    var opened by remember { mutableStateOf<Badge?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
        val unlocked = Badge.entries.count { it.unlocked(stats) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mes badges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            Text("$unlocked / ${Badge.entries.size}", style = MaterialTheme.typography.labelLarge, color = c.reward)
        }
        Badge.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
                row.forEach { badge -> BadgeTile(badge, stats, Modifier.weight(1f)) { opened = badge } }
            }
        }
    }
    opened?.let { badge -> BadgeDetail(badge, stats, unlockedAt[badge], onDismiss = { opened = null }) }
}

/** Tapped badge: big illustration, the condition in plain words, progress, and when it was earned. */
@Composable
private fun BadgeDetail(badge: Badge, stats: PlayerStats, unlockedAt: Long?, onDismiss: () -> Unit) {
    val c = RideakTheme.colors
    val unlocked = badge.unlocked(stats)
    val progress = badge.progress(stats)
    val fraction = (progress.toFloat() / badge.goal).coerceIn(0f, 1f)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .background(c.surface, RideakTheme.shapes.large)
                .border(1.dp, (if (unlocked) c.reward else c.lightSurface).copy(alpha = 0.5f), RideakTheme.shapes.large)
                .padding(RideakTheme.spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
        ) {
            Image(
                painterResource(badge.art),
                contentDescription = null,
                colorFilter = if (unlocked) null else greyscale,
                modifier = Modifier.size(160.dp).alpha(if (unlocked) 1f else 0.6f),
            )
            Text(
                badge.title.uppercase(),
                style = RideakTheme.typography.impact.copy(fontSize = 32.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
                color = if (unlocked) c.reward else c.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(badge.description, style = MaterialTheme.typography.titleMedium, color = c.textPrimary, textAlign = TextAlign.Center)
            Box(
                Modifier.fillMaxWidth().height(8.dp).background(c.surfaceRaised, RideakTheme.shapes.pill)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
            ) {
                Box(Modifier.fillMaxWidth(fraction).height(8.dp).background(if (unlocked) c.nature else c.reward, RideakTheme.shapes.pill))
            }
            Text(
                when {
                    unlocked && unlockedAt != null -> "Débloqué le ${formatDay(unlockedAt)}"
                    unlocked -> "Débloqué"
                    else -> "$progress / ${badge.goal}"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (unlocked) c.nature else c.textSecondary,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = RideakTheme.spacing.touchTarget)) {
                Text("Fermer", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            }
        }
    }
}

private val greyscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

@Composable
private fun BadgeTile(badge: Badge, stats: PlayerStats, modifier: Modifier, onClick: () -> Unit) {
    val c = RideakTheme.colors
    val unlocked = badge.unlocked(stats)
    val progress = badge.progress(stats)
    Column(
        modifier.clip(RideakTheme.shapes.medium).background(c.surface)
            .clickable(onClickLabel = "Voir le badge", onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .clearAndSetSemantics {
                onClick(label = "Voir le badge") { onClick(); true }
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

/**
 * End-of-ride rewards: XP earned and badges unlocked by this ride. With [celebrate] (just after the
 * ride) the XP counts up and each badge bounces in with a yellow flash.
 */
@Composable
fun RideRewards(xp: Int, badges: List<Badge>, modifier: Modifier = Modifier, celebrate: Boolean = false) {
    if (xp <= 0 && badges.isEmpty()) return
    val c = RideakTheme.colors
    val shownXp = remember { Animatable(if (celebrate) 0f else xp.toFloat()) }
    val bounce = remember { Animatable(if (celebrate) 0.6f else 1f) }
    val flash = remember { Animatable(if (celebrate) 1f else 0f) }
    LaunchedEffect(xp) {
        shownXp.animateTo(xp.toFloat(), tween(1_000, delayMillis = 300))
        bounce.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) { flash.animateTo(0f, tween(1_200, delayMillis = 1_300)) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (xp > 0) {
            Text(
                "+${shownXp.value.roundToInt()} XP",
                style = RideakTheme.typography.impact.copy(fontSize = 26.sp, shadow = Shadow(Color.Black, Offset(2f, 3f), 4f)),
                color = c.reward,
                modifier = Modifier.semantics { contentDescription = "+$xp XP" },
            )
        }
        badges.forEach { badge ->
            Row(
                Modifier.fillMaxWidth()
                    .graphicsLayer {
                        scaleX = bounce.value
                        scaleY = bounce.value
                    }
                    .background(lerp(c.surface, c.reward.copy(alpha = 0.5f), flash.value), RideakTheme.shapes.medium)
                    .border((1 + 2 * flash.value).dp, c.reward.copy(alpha = 0.6f + 0.4f * flash.value), RideakTheme.shapes.medium)
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
