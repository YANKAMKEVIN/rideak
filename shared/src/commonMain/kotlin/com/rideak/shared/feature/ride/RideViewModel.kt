package com.rideak.shared.feature.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.location.LocationSourceSelector
import com.rideak.data.recorder.RecorderState
import com.rideak.data.recorder.RideRecorder
import com.rideak.exploration.RideId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface RideEvent {
    data class Finished(val rideId: RideId) : RideEvent
    data object SpotSaved : RideEvent
    data object SpotNeedsFix : RideEvent
}

class RideViewModel(
    private val recorder: RideRecorder,
    selector: LocationSourceSelector,
) : ViewModel() {
    val state: StateFlow<RecorderState> = recorder.state
    val simulated: StateFlow<Boolean> = selector.useSimulation

    private val _events = Channel<RideEvent>(Channel.BUFFERED)
    val events: Flow<RideEvent> = _events.receiveAsFlow()

    private var autoStarted = false

    init {
        // A ride can end from here or from the notification: both lead to the summary.
        viewModelScope.launch {
            var current: RideId? = null
            recorder.state.collect { s ->
                when (s) {
                    is RecorderState.Active -> current = s.rideId
                    RecorderState.Idle -> current?.let { _events.send(RideEvent.Finished(it)) }.also { current = null }
                }
            }
        }
    }

    /** Opening the ride screen starts a ride once; never restarts one that just ended. */
    fun startOnce() {
        if (autoStarted) return
        autoStarted = true
        viewModelScope.launch { recorder.start() }
    }

    fun pause() = launch { recorder.pause() }
    fun resume() = launch { recorder.resume() }
    fun stop() = launch { recorder.stop() }

    fun dropSpot() = launch {
        _events.send(if (recorder.dropSpot() != null) RideEvent.SpotSaved else RideEvent.SpotNeedsFix)
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
