package com.rideak.data.recorder

import com.rideak.data.RideRepository
import com.rideak.data.location.LocationSource
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideMetrics
import com.rideak.ride.RideMetricsCalculator
import com.rideak.ride.RideMetricsState
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import com.rideak.ride.Spot
import com.rideak.ride.SpotId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class GpsSignal { SEARCHING, WEAK, GOOD }

sealed interface RecorderState {
    data object Idle : RecorderState

    data class Active(
        val rideId: RideId,
        /** RECORDING or PAUSED. */
        val status: RideStatus,
        val startedAtEpochMillis: Long,
        /** Recording time, pauses excluded. */
        val activeDurationMillis: Long,
        val metrics: RideMetrics,
        val signal: GpsSignal,
        val spotCount: Int,
        /** Restored paused after the app was killed during a ride. */
        val recovered: Boolean,
        /** Usable positions of the ride so far, for the live track preview. */
        val track: List<GeoPoint> = emptyList(),
    ) : RecorderState
}

/** Keeps the process alive while recording (Android foreground service). No-op where not needed. */
interface RideForegroundController {
    fun onRideActive()
    fun onRideEnded()
}

object NoForegroundController : RideForegroundController {
    override fun onRideActive() = Unit
    override fun onRideEnded() = Unit
}

data class RecorderConfig(
    val flushEveryPoints: Int = 10,
    val flushEveryMillis: Long = 5_000,
    val tickMillis: Long = 1_000,
    val signalLostAfterMillis: Long = 10_000,
    val weakAccuracyMeters: Float = 25f,
)

/**
 * Records one ride at a time: collects fixes, updates live metrics, and writes points to storage
 * in small batches so at most a few seconds are lost if the process dies. Lives in an application
 * scope, independently of any screen. All mutations are serialized by [mutex].
 */
class RideRecorder(
    private val repository: RideRepository,
    private val locationSource: () -> LocationSource,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val newId: () -> String,
    private val foreground: RideForegroundController = NoForegroundController,
    private val calculator: RideMetricsCalculator = RideMetricsCalculator(),
    private val config: RecorderConfig = RecorderConfig(),
) {
    private class Session(
        val rideId: RideId,
        val startedAt: Long,
        var segment: Int,
        var activeBeforeSegment: Long,
        /** Null while paused. */
        var segmentStartedAt: Long?,
        var metrics: RideMetricsState,
        var spotCount: Int,
        var recovered: Boolean,
        var lastFixReceivedAt: Long? = null,
        var lastFlushAt: Long,
        val buffer: MutableList<TrackPoint> = mutableListOf(),
        val positions: MutableList<GeoPoint> = mutableListOf(),
    ) {
        val recording get() = segmentStartedAt != null
        fun activeDuration(now: Long) = activeBeforeSegment + (segmentStartedAt?.let { now - it } ?: 0)
    }

    private val mutex = Mutex()
    private var session: Session? = null
    private var locationJob: Job? = null
    private var tickJob: Job? = null

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    /** Restores a ride left unfinished by a killed process, paused, so the rider chooses what to do. */
    suspend fun recoverUnfinished() = mutex.withLock {
        if (session != null) return@withLock
        val ride = repository.unfinishedRide() ?: return@withLock
        val segments = repository.track(ride.id)
        val now = clock()
        session = Session(
            rideId = ride.id,
            startedAt = ride.startedAtEpochMillis,
            segment = (segments.size - 1).coerceAtLeast(0),
            activeBeforeSegment = ride.activeDurationMillis,
            segmentStartedAt = null,
            metrics = calculator.newSegment(calculator.compute(segments)),
            spotCount = ride.spotCount,
            recovered = true,
            lastFlushAt = now,
        ).also { recovered -> segments.flatten().filter(::drawable).mapTo(recovered.positions) { it.position } }
        flush(RideStatus.PAUSED)
        publish()
    }

    suspend fun start(): RideId = mutex.withLock {
        session?.let { return@withLock it.rideId }
        val now = clock()
        val s = Session(
            rideId = RideId(newId()),
            startedAt = now,
            segment = 0,
            activeBeforeSegment = 0,
            segmentStartedAt = now,
            metrics = RideMetricsState(),
            spotCount = 0,
            recovered = false,
            lastFlushAt = now,
        )
        repository.createRide(s.summary(RideStatus.RECORDING, now))
        session = s
        startCollecting()
        startTicking()
        foreground.onRideActive()
        publish()
        s.rideId
    }

    suspend fun pause() = mutex.withLock {
        val s = session?.takeIf { it.recording } ?: return@withLock
        val now = clock()
        s.activeBeforeSegment = s.activeDuration(now)
        s.segmentStartedAt = null
        locationJob?.cancel()
        flush(RideStatus.PAUSED)
        s.metrics = calculator.newSegment(s.metrics)
        publish()
    }

    suspend fun resume() = mutex.withLock {
        val s = session?.takeUnless { it.recording } ?: return@withLock
        s.segment += 1
        s.segmentStartedAt = clock()
        s.recovered = false
        startCollecting()
        startTicking()
        foreground.onRideActive()
        publish()
    }

    /** Finishes the ride and returns its id (null if nothing was being recorded). */
    suspend fun stop(): RideId? = mutex.withLock {
        val s = session ?: return@withLock null
        val now = clock()
        s.activeBeforeSegment = s.activeDuration(now)
        s.segmentStartedAt = null
        locationJob?.cancel()
        tickJob?.cancel()
        flush(RideStatus.FINISHED, endedAt = now)
        session = null
        _state.value = RecorderState.Idle
        foreground.onRideEnded()
        s.rideId
    }

    /** ❤️: saves the current position as a spot. Null when no fix is known yet. */
    suspend fun dropSpot(): Spot? = mutex.withLock {
        val s = session ?: return@withLock null
        val position = s.metrics.lastFix?.position ?: return@withLock null
        val spot = Spot(SpotId(newId()), s.rideId, position, clock())
        repository.addSpot(spot)
        s.spotCount += 1
        publish()
        spot
    }

    private fun startCollecting() {
        locationJob?.cancel()
        locationJob = scope.launch {
            locationSource().locations()
                .catch { /* Permission revoked or provider failure: surfaces as SEARCHING. */ }
                .collect { point -> mutex.withLock { onFix(point) } }
        }
    }

    private fun startTicking() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (true) {
                delay(config.tickMillis)
                mutex.withLock {
                    val s = session ?: return@withLock
                    if (s.recording && clock() - s.lastFlushAt >= config.flushEveryMillis) flush(RideStatus.RECORDING)
                    publish()
                }
            }
        }
    }

    private suspend fun onFix(point: TrackPoint) {
        val s = session?.takeIf { it.recording } ?: return
        s.metrics = calculator.add(s.metrics, point)
        s.lastFixReceivedAt = clock()
        s.buffer += point
        if (drawable(point)) s.positions += point.position
        if (s.buffer.size >= config.flushEveryPoints) flush(RideStatus.RECORDING)
        publish()
    }

    private suspend fun flush(status: RideStatus, endedAt: Long? = null) {
        val s = session ?: return
        val now = clock()
        if (s.buffer.isNotEmpty()) {
            repository.appendPoints(s.rideId, s.segment, s.buffer.toList())
            s.buffer.clear()
        }
        repository.saveSummary(s.summary(status, now, endedAt))
        s.lastFlushAt = now
    }

    private fun publish() {
        val s = session ?: return
        val now = clock()
        _state.value = RecorderState.Active(
            rideId = s.rideId,
            status = if (s.recording) RideStatus.RECORDING else RideStatus.PAUSED,
            startedAtEpochMillis = s.startedAt,
            activeDurationMillis = s.activeDuration(now),
            metrics = s.metrics.metrics,
            signal = signal(s, now),
            spotCount = s.spotCount,
            recovered = s.recovered,
            track = s.positions.toList(),
        )
    }

    private fun drawable(point: TrackPoint) =
        point.horizontalAccuracyMeters.let { it == null || it <= config.weakAccuracyMeters }

    private fun signal(s: Session, now: Long): GpsSignal {
        val receivedAt = s.lastFixReceivedAt ?: return GpsSignal.SEARCHING
        if (now - receivedAt > config.signalLostAfterMillis) return GpsSignal.SEARCHING
        val accuracy = s.metrics.lastFix?.horizontalAccuracyMeters ?: return GpsSignal.GOOD
        return if (accuracy > config.weakAccuracyMeters) GpsSignal.WEAK else GpsSignal.GOOD
    }

    private fun Session.summary(status: RideStatus, now: Long, endedAt: Long? = null): RideSummary {
        val m = metrics.metrics
        return RideSummary(
            id = rideId,
            startedAtEpochMillis = startedAt,
            endedAtEpochMillis = endedAt,
            status = status,
            activeDurationMillis = activeDuration(now),
            distanceMeters = m.distanceMeters,
            movingMillis = m.movingMillis,
            maxSpeedMps = m.maxSpeedMps,
            elevationGainMeters = m.elevationGainMeters,
            spotCount = spotCount,
        )
    }
}
