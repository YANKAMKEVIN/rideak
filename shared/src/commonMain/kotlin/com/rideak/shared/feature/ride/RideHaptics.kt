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

    /** A new place: two firm pulses. */
    fun placeDiscovered() = play(listOf(HapticFeedbackType.Confirm, HapticFeedbackType.Confirm), gapMillis = 160)

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
