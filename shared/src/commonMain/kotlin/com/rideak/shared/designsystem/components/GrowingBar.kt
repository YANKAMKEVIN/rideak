package com.rideak.shared.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme

/**
 * Progress bar that fills from the value it last showed to [fraction] in 800 ms (from empty the
 * first time). When [stage] goes up (a milestone or a level crossed) it fills to the end, flashes
 * yellow, then starts again from empty. The last value shown survives tab switches and rotation.
 */
@Composable
fun GrowingBar(fraction: Float, color: Color, stage: Int, modifier: Modifier = Modifier) {
    val colors = RideakTheme.colors
    var lastFraction by rememberSaveable { mutableFloatStateOf(0f) }
    var lastStage by rememberSaveable { mutableIntStateOf(stage) }
    val fill = remember { Animatable(lastFraction) }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(fraction, stage) {
        val target = fraction.coerceIn(0f, 1f)
        // Nothing shown yet (screen still loading its data): no flash, just the fill.
        val seen = lastFraction > 0f || lastStage > 0
        if (stage > lastStage && seen) {
            fill.animateTo(1f, tween(400))
            flash.snapTo(1f)
            flash.animateTo(0f, tween(700))
            fill.snapTo(0f)
            fill.animateTo(target, tween(600))
        } else {
            fill.animateTo(target, tween(800))
        }
        lastFraction = target
        lastStage = stage
    }
    Box(
        modifier.fillMaxWidth().height(8.dp)
            .graphicsLayer { scaleY = 1f + 0.5f * flash.value }
            .background(colors.surfaceRaised, RideakTheme.shapes.pill),
    ) {
        Box(Modifier.fillMaxWidth(fill.value).fillMaxHeight().background(color, RideakTheme.shapes.pill))
        if (flash.value > 0f) {
            Box(Modifier.fillMaxWidth().fillMaxHeight().background(colors.reward.copy(alpha = flash.value), RideakTheme.shapes.pill))
        }
    }
}
