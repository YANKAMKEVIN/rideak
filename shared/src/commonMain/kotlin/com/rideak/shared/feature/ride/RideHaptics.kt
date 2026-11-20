package com.rideak.shared.feature.ride

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What the rider feels through the handlebar, eyes on the road: each ride event has its own
 * pattern, so it can be told apart without looking at the screen.
 */
class RideHaptics(private val haptic: HapticFeedback, private val scope: CoroutineScope) {
    /** A cell revealed for the first time: one light tick. */
    fun zoneRevealed() = play(listOf(HapticFeedbackType.SegmentTick))

    /** A new place: three firm pulses. */
    fun placeDiscovered() = play(listOf(HapticFeedbackType.Confirm, HapticFeedbackType.Confirm, HapticFeedbackType.Confirm))

    /** Pause: one short pulse. */
    fun paused() = play(listOf(HapticFeedbackType.ToggleOff))

    /** Resume: two short pulses. */
    fun resumed() = play(listOf(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOn), gapMillis = 100)

    /** ❤️ spot saved: a heartbeat, strong then light. */
    fun spotSaved() = play(listOf(HapticFeedbackType.LongPress, HapticFeedbackType.ContextClick), gapMillis = 140)

    /**
     * Ride saved: one long buzz, made of close pulses. The first is played at once: the screen
     * (and this scope) goes away as the summary opens.
     */
    fun rideSaved() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            repeat(2) {
                delay(60)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    private fun play(pulses: List<HapticFeedbackType>, gapMillis: Long = 120) {
        scope.launch {
            pulses.forEachIndexed { i, pulse ->
                if (i > 0) delay(gapMillis)
                haptic.performHapticFeedback(pulse)
            }
        }
    }
}

@Composable
fun rememberRideHaptics(): RideHaptics {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    return remember(haptic, scope) { RideHaptics(haptic, scope) }
}
