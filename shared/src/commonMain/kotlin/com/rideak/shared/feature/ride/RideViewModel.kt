package com.rideak.shared.feature.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.data.location.LocationSourceSelector
import com.rideak.data.recorder.RecorderState
import com.rideak.data.recorder.RideRecorder
import com.rideak.exploration.LiveDiscovery
import com.rideak.exploration.Place
import com.rideak.exploration.RideId
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.places
import com.rideak.shared.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RideEvent {
    data class Finished(val rideId: RideId) : RideEvent
    data object SpotSaved : RideEvent
    data object SpotNeedsFix : RideEvent

    /** Cells this ride just revealed for the first time (never ridden before). */
    data class ZonesRevealed(val count: Int) : RideEvent

    /** A real place this ride just discovered. */
    data class PlaceDiscovered(val place: Place) : RideEvent
}

class RideViewModel(
    private val recorder: RideRecorder,
    selector: LocationSourceSelector,
    settings: AppSettings,
    private val repository: RideRepository,
    private val regions: RegionSource,
) : ViewModel() {
    val state: StateFlow<RecorderState> = recorder.state
    val simulated: StateFlow<Boolean> = selector.useSimulation
    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn

    private val _events = Channel<RideEvent>(Channel.BUFFERED)
    val events: Flow<RideEvent> = _events.receiveAsFlow()

    private var autoStarted = false

    private val _revealedCells = MutableStateFlow(0)

    /** Cells revealed so far by the ride being recorded, for the live counter. */
    val revealedCells: StateFlow<Int> = _revealedCells.asStateFlow()

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
        viewModelScope.launch { followDiscoveries() }
    }

    /**
     * Compares the growing track with every earlier ride, every few seconds, and announces what is new.
     * The first pass of a ride this screen did not start (reopened, recovered) only sets the counter:
     * what was found before must not pop up again.
     */
    private suspend fun followDiscoveries() {
        var live: LiveDiscovery? = null
        var rideId: RideId? = null
        var announce = false
        var lastSize = -1
        recorder.state.filterIsInstance<RecorderState.Active>().conflate().collect { s ->
            if (s.rideId != rideId) {
                rideId = s.rideId
                announce = autoStarted
                lastSize = -1
                val before = repository.finishedSegments()
                val places = regions.places()
                live = withContext(Dispatchers.Default) { LiveDiscovery(before, places) }
            }
            val discovery = live ?: return@collect
            if (s.track.isEmpty() || s.track.size == lastSize) return@collect
            lastSize = s.track.size
            val update = withContext(Dispatchers.Default) { discovery.update(listOf(s.track)) }
            _revealedCells.value = update.revealedCells
            if (announce) {
                update.newPlaces.forEach { _events.send(RideEvent.PlaceDiscovered(it)) }
                if (update.newCells > 0) _events.send(RideEvent.ZonesRevealed(update.newCells))
            }
            announce = true
            delay(DISCOVERY_INTERVAL_MILLIS)
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

    private companion object {
        /** The whole track is replayed at each pass: a few seconds between passes is plenty at bike speed. */
        const val DISCOVERY_INTERVAL_MILLIS = 3_000L
    }
}
