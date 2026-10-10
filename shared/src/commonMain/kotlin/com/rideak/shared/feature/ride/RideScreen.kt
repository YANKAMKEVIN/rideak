package com.rideak.shared.feature.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.data.recorder.GpsSignal
import com.rideak.data.recorder.RecorderState
import com.rideak.exploration.RideId
import com.rideak.ride.RideStatus
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.components.RideMetric
import com.rideak.shared.designsystem.icons.RideakIcons
import com.rideak.shared.format.formatDuration
import com.rideak.shared.format.formatKilometers
import com.rideak.shared.format.speedKmhRounded
import com.rideak.shared.permission.LocationAccess
import com.rideak.shared.permission.LocationPermissionController
import com.rideak.shared.permission.rememberLocationPermission
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

/**
 * Ride mode (mockup "3. EN SORTIE"): readable at a glance, few controls, large touch targets.
 * Leaving the screen does not stop the ride; it keeps recording in the background.
 */
@Composable
fun RideScreen(
    onFinished: (RideId) -> Unit,
    onMinimize: () -> Unit,
    viewModel: RideViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val simulated by viewModel.simulated.collectAsStateWithLifecycle()
    val permission = rememberLocationPermission()
    var acceptedApproximate by remember { mutableStateOf(false) }
    // Big in-screen confirmation rather than a small snackbar: readable at a glance while riding.
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RideEvent.Finished -> onFinished(event.rideId)
                RideEvent.SpotSaved -> feedback = "❤️ Spot enregistré"
                RideEvent.SpotNeedsFix -> feedback = "Position GPS pas encore trouvée"
            }
        }
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            delay(2_500)
            feedback = null
        }
    }

    Box(
        Modifier.fillMaxSize().background(RideakTheme.colors.night)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        val active = state as? RecorderState.Active
        val canRecord = simulated || permission.access == LocationAccess.PRECISE ||
            (permission.access == LocationAccess.APPROXIMATE && acceptedApproximate)
        when {
            active != null -> ActiveRide(active, simulated, feedback, viewModel, onMinimize)
            canRecord -> {
                LaunchedEffect(Unit) { viewModel.startOnce() }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RideakTheme.colors.exploration)
                }
            }
            else -> PermissionGate(permission, onAcceptApproximate = { acceptedApproximate = true }, onCancel = onMinimize)
        }
    }
}

@Composable
private fun ActiveRide(
    state: RecorderState.Active,
    simulated: Boolean,
    feedback: String?,
    viewModel: RideViewModel,
    onMinimize: () -> Unit,
) {
    var confirmStop by remember { mutableStateOf(false) }
    val paused = state.status == RideStatus.PAUSED
    val colors = RideakTheme.colors

    Column(Modifier.fillMaxSize().padding(RideakTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinimize, modifier = Modifier.size(RideakTheme.spacing.touchTarget)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Réduire, la sortie continue", tint = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (simulated) StatusChip("SIMULATION", colors.reward)
            Spacer(Modifier.size(RideakTheme.spacing.s))
            SignalChip(state.signal)
        }

        if (state.recovered) {
            Text(
                "Ta sortie a été interrompue. Reprends-la ou termine-la.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.reward,
            )
        }

        Row(verticalAlignment = Alignment.Top) {
            Speed(state)
            Spacer(Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
                RideMetric(formatKilometers(state.metrics.distanceMeters), "km", "distance", spokenUnit = "kilomètres")
                RideMetric(formatDuration(state.activeDurationMillis), "", "durée")
                RideMetric("${state.metrics.elevationGainMeters.toInt()}", "m", "dénivelé +", spokenUnit = "mètres")
            }
        }

        // The map arrives with the MapLibre spike; meanwhile this area states what is happening.
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .background(colors.surface, RideakTheme.shapes.large)
                .border(1.dp, colors.lightSurface.copy(alpha = 0.2f), RideakTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            if (feedback != null) {
                Text(
                    feedback,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.mangaWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(colors.night.copy(alpha = 0.85f), RideakTheme.shapes.pill)
                        .border(2.dp, colors.action, RideakTheme.shapes.pill)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            } else if (paused) {
                Text(
                    "EN PAUSE",
                    style = RideakTheme.typography.impact,
                    color = colors.reward,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            } else {
                Text(
                    "La carte arrive au prochain jalon",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(RideakTheme.spacing.l),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (paused) {
                RoundAction(RideakIcons.Stop, "Terminer la sortie", colors.surface, colors.action, 72.dp, outline = colors.action) {
                    confirmStop = true
                }
                RoundAction(RideakIcons.Play, "Reprendre la sortie", colors.nature, colors.onAccent, 96.dp) { viewModel.resume() }
            } else {
                Spacer(Modifier.size(72.dp))
                RoundAction(RideakIcons.Pause, "Mettre en pause", colors.action, Color.White, 96.dp) { viewModel.pause() }
            }
            RoundAction(
                RideakIcons.Heart,
                "J'aime cet endroit (${state.spotCount} spots dans cette sortie)",
                colors.surface, colors.action, 72.dp,
            ) { viewModel.dropSpot() }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Terminer la sortie ?") },
            text = { Text("Elle sera enregistrée dans tes sorties.") },
            confirmButton = { TextButton(onClick = { confirmStop = false; viewModel.stop() }) { Text("Terminer") } },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Continuer") } },
        )
    }
}

@Composable
private fun Speed(state: RecorderState.Active) {
    val searching = state.signal == GpsSignal.SEARCHING && state.metrics.currentSpeedMps == 0.0
    val value = if (searching) "--" else speedKmhRounded(state.metrics.currentSpeedMps).toString()
    val shadow = Shadow(Color.Black, Offset(3f, 4f), 6f)
    Column(Modifier.clearAndSetSemantics { contentDescription = if (searching) "Vitesse inconnue" else "$value kilomètres heure" }) {
        Text(
            value,
            style = RideakTheme.typography.impact.copy(fontSize = 112.sp, lineHeight = 112.sp, shadow = shadow),
            color = RideakTheme.colors.mangaWhite,
        )
        Text("KM/H", style = RideakTheme.typography.impact.copy(fontSize = 28.sp, lineHeight = 30.sp), color = RideakTheme.colors.mangaWhite)
    }
}

@Composable
private fun SignalChip(signal: GpsSignal) {
    val colors = RideakTheme.colors
    when (signal) {
        GpsSignal.GOOD -> StatusChip("GPS OK", colors.nature)
        GpsSignal.WEAK -> StatusChip("Signal faible", colors.reward)
        GpsSignal.SEARCHING -> StatusChip("Recherche GPS…", colors.lightSurface)
    }
}

/** Status is spelled out, colour only reinforces it. */
@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.6f), RideakTheme.shapes.pill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    size: Dp,
    outline: Color? = null,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(size).let { if (outline != null) it.border(2.dp, outline, RideakTheme.shapes.pill) else it },
        shape = RideakTheme.shapes.pill,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(size * 0.42f))
    }
}

@Composable
private fun PermissionGate(permission: LocationPermissionController, onAcceptApproximate: () -> Unit, onCancel: () -> Unit) {
    val colors = RideakTheme.colors
    Column(
        Modifier.fillMaxSize().padding(RideakTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m, Alignment.CenterVertically),
    ) {
        Text("On a besoin de ta position", style = RideakTheme.typography.impact.copy(fontSize = 32.sp, lineHeight = 36.sp), color = colors.mangaWhite)
        Text(
            "RIDEAK enregistre ton trajet pendant la sortie pour mesurer ta distance et dévoiler ton monde. " +
                "Ta position reste sur ton téléphone.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
        )
        when (permission.access) {
            LocationAccess.APPROXIMATE -> {
                Text(
                    "Tu as accordé une position approximative (à ~2 km près) : la distance et l'exploration seront fausses. " +
                        "Active la position précise pour une vraie sortie.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.reward,
                )
                AdventureButton("ACTIVER LA POSITION PRÉCISE", onClick = permission.openSettings)
                TextButton(onClick = onAcceptApproximate) { Text("Continuer quand même") }
            }
            LocationAccess.DENIED -> {
                Text("L'accès à la position est refusé. Tu peux l'activer dans les réglages.", style = MaterialTheme.typography.bodyLarge, color = colors.reward)
                AdventureButton("OUVRIR LES RÉGLAGES", onClick = permission.openSettings)
            }
            else -> AdventureButton("AUTORISER LA POSITION", onClick = permission.request)
        }
        TextButton(onClick = onCancel) { Text("Pas maintenant") }
    }
}
