package com.rideak.data.location

import com.rideak.exploration.TrackPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stream of raw fixes, converted to [TrackPoint] at the platform boundary: no Android `Location`
 * or `CLLocation` ever leaves the platform source sets. Cold: collecting starts updates, cancelling stops them.
 */
fun interface LocationSource {
    fun locations(): Flow<TrackPoint>
}

/** Picks the real GPS or the simulated ride (debug tool). */
class LocationSourceSelector(
    private val device: LocationSource,
    private val simulated: LocationSource,
) {
    private val _useSimulation = MutableStateFlow(false)
    val useSimulation: StateFlow<Boolean> = _useSimulation

    fun setUseSimulation(enabled: Boolean) {
        _useSimulation.value = enabled
    }

    fun current(): LocationSource = if (_useSimulation.value) simulated else device
}
