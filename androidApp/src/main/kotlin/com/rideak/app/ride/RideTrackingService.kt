package com.rideak.app.ride

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.rideak.app.MainActivity
import com.rideak.app.R
import com.rideak.data.recorder.RecorderState
import com.rideak.data.recorder.RideForegroundController
import com.rideak.data.recorder.RideRecorder
import com.rideak.ride.RideStatus
import com.rideak.shared.format.formatDuration
import com.rideak.shared.format.formatKilometers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Foreground service (type `location`) that keeps the process alive while a ride is active,
 * screen off or app in background. The recording itself lives in [RideRecorder]; this service
 * only owns the notification and its Pause / Resume / Finish actions.
 */
class RideTrackingService : Service() {
    private val recorder: RideRecorder by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!observing) {
            observing = true
            running = true
            createChannel()
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification(recorder.state.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
            scope.launch {
                recorder.state.collect { state ->
                    if (state is RecorderState.Active) {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(state))
                    } else {
                        ServiceCompat.stopForeground(this@RideTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { recorder.pause() }
            ACTION_RESUME -> scope.launch { recorder.resume() }
            ACTION_STOP -> scope.launch { recorder.stop() }
        }
        // If the process dies, the ride is recovered (paused) at next launch rather than restarted blindly.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Sortie en cours", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Distance et durée de la sortie en cours"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(state: RecorderState) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .apply {
            val active = state as? RecorderState.Active
            val paused = active?.status == RideStatus.PAUSED
            setContentTitle(if (paused) "Sortie en pause" else "Sortie en cours")
            if (active != null) {
                setContentText("${formatKilometers(active.metrics.distanceMeters)} km · ${formatDuration(active.activeDurationMillis)}")
            }
            if (paused) addAction(0, "Reprendre", action(ACTION_RESUME)) else addAction(0, "Pause", action(ACTION_PAUSE))
            addAction(0, "Terminer", action(ACTION_STOP))
        }
        .build()

    private fun action(name: String) = PendingIntent.getService(
        this, name.hashCode(), Intent(this, RideTrackingService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val CHANNEL_ID = "ride"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PAUSE = "com.rideak.app.ride.PAUSE"
        private const val ACTION_RESUME = "com.rideak.app.ride.RESUME"
        private const val ACTION_STOP = "com.rideak.app.ride.STOP"

        @Volatile
        var running = false
            private set
    }
}

class AndroidRideForeground(private val context: Context) : RideForegroundController {
    override fun onRideActive() {
        // Already running (e.g. resumed from its own notification): nothing to start.
        if (RideTrackingService.running) return
        ContextCompat.startForegroundService(context, Intent(context, RideTrackingService::class.java))
    }

    // The service stops itself when the recorder becomes idle.
    override fun onRideEnded() = Unit
}
