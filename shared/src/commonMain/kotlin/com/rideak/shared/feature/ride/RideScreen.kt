package com.rideak.shared.feature.ride

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.data.recorder.GpsSignal
import com.rideak.data.recorder.RecorderState
import com.rideak.exploration.RideId
import com.rideak.ride.RideStatus
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.feature.map.RideMap
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

    Box(Modifier.fillMaxSize().background(RideakTheme.colors.night)) {
        val active = state as? RecorderState.Active
        val canRecord = simulated || permission.access == LocationAccess.PRECISE ||
            (permission.access == LocationAccess.APPROXIMATE && acceptedApproximate)
        when {
            active != null -> ActiveRide(
                state = active,
                simulated = simulated,
                feedback = feedback,
                viewModel = viewModel,
                locationOff = !simulated && !permission.servicesEnabled,
                onOpenLocationSettings = permission.openLocationSettings,
                onResume = {
                    // A recovered ride can be resumed before any permission was granted.
                    when {
                        simulated || permission.access == LocationAccess.PRECISE || permission.access == LocationAccess.APPROXIMATE ->
                            viewModel.resume()
                        permission.access == LocationAccess.DENIED -> permission.openSettings()
                        else -> permission.request()
                    }
                },
                onMinimize = onMinimize,
            )
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
    locationOff: Boolean,
    onOpenLocationSettings: () -> Unit,
    onResume: () -> Unit,
    onMinimize: () -> Unit,
) {
    var confirmStop by remember { mutableStateOf(false) }
    val paused = state.status == RideStatus.PAUSED
    val colors = RideakTheme.colors

    val notices: @Composable () -> Unit = {
        if (locationOff) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "La localisation de ton téléphone est coupée : aucune position ne peut être enregistrée.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenLocationSettings) { Text("Activer") }
            }
        } else if (state.recovered) {
            Text("Ta sortie a été interrompue. Reprends-la ou termine-la.", style = MaterialTheme.typography.bodyMedium, color = colors.reward)
        }
    }
    val topBar: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinimize, modifier = Modifier.size(RideakTheme.spacing.touchTarget)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Réduire, la sortie continue", tint = colors.textPrimary)
            }
            Spacer(Modifier.weight(1f))
            if (simulated) StatusChip("SIMULATION", colors.reward)
            Spacer(Modifier.size(RideakTheme.spacing.s))
            when {
                locationOff -> StatusChip("Localisation désactivée", colors.action)
                // GPS is off while paused: no signal status to show.
                !paused -> SignalChip(state.signal)
            }
        }
    }
    val mapOverlay: @Composable () -> Unit = {
        when {
            feedback != null -> FeedbackPill(feedback)
            paused -> Text(
                "EN PAUSE",
                style = RideakTheme.typography.impact.copy(fontSize = 56.sp, shadow = Shadow(Color.Black, Offset(3f, 4f), 8f)),
                color = colors.reward,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            // On its own pill: grey text straight on a light map tile was unreadable in daylight.
            state.track.isEmpty() -> Text(
                "En attente du GPS…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.mangaWhite,
                modifier = Modifier
                    .background(colors.night.copy(alpha = 0.9f), RideakTheme.shapes.pill)
                    .border(2.dp, colors.lightSurface, RideakTheme.shapes.pill)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelWidth = maxWidth * 0.45f
        // The real map follows the rider; insets keep its attribution above the controls.
        RideMap(
            track = listOf(state.track),
            modifier = Modifier.fillMaxSize(),
            follow = true,
            insets = PaddingValues(top = 230.dp, bottom = 130.dp),
        )

        if (maxWidth > maxHeight) {
            // Landscape (phone mounted sideways): stats panel on the left, controls stacked on the right.
            Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Column(
                    Modifier.fillMaxHeight().width(panelWidth).background(colors.night.copy(alpha = 0.92f))
                        .padding(RideakTheme.spacing.m),
                    verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
                ) {
                    topBar()
                    notices()
                    SpeedAndMetrics(state)
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { mapOverlay() }
                Controls(paused, state.spotCount, vertical = true, viewModel, onResume) { confirmStop = true }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(colors.night, SlantedBottomShape(28.dp))
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = RideakTheme.spacing.m, end = RideakTheme.spacing.m, bottom = 36.dp),
                ) {
                    topBar()
                    notices()
                    SpeedAndMetrics(state)
                }
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { mapOverlay() }
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.night.copy(alpha = 0.95f))))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(vertical = RideakTheme.spacing.m),
                ) {
                    Controls(paused, state.spotCount, vertical = false, viewModel, onResume) { confirmStop = true }
                }
            }
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

/** "⚡ 23 KM/H" in the manga voice, then distance / duration / climb with their icons (mockup layout). */
@Composable
private fun SpeedAndMetrics(state: RecorderState.Active) {
    val colors = RideakTheme.colors
    val searching = state.signal == GpsSignal.SEARCHING && state.metrics.currentSpeedMps == 0.0
    val speed = if (searching) "--" else speedKmhRounded(state.metrics.currentSpeedMps).toString()
    val shadow = Shadow(Color.Black, Offset(4f, 5f), 6f)
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).graphicsLayer(rotationZ = -4f)
                .clearAndSetSemantics { contentDescription = if (searching) "Vitesse inconnue" else "$speed kilomètres heure" },
            verticalAlignment = Alignment.Top,
        ) {
            Icon(RideakIcons.Lightning, contentDescription = null, tint = colors.action, modifier = Modifier.padding(top = 22.dp).size(36.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(speed, style = RideakTheme.typography.impact.copy(fontSize = 128.sp, lineHeight = 116.sp, shadow = shadow), color = colors.mangaWhite)
                Text("KM/H", style = RideakTheme.typography.impact.copy(fontSize = 40.sp, lineHeight = 40.sp, shadow = shadow), color = colors.mangaWhite)
            }
        }
        Box(Modifier.padding(horizontal = RideakTheme.spacing.m).width(1.dp).fillMaxHeight().background(colors.lightSurface.copy(alpha = 0.35f)))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricLine(RideakIcons.Stopwatch, colors.mangaWhite, formatKilometers(state.metrics.distanceMeters), "km", "Distance : ${formatKilometers(state.metrics.distanceMeters)} kilomètres")
            MetricLine(RideakIcons.Timer, colors.exploration, formatDuration(state.activeDurationMillis), "", "Durée : ${formatDuration(state.activeDurationMillis)}")
            MetricLine(RideakIcons.Mountain, colors.exploration, "${state.metrics.elevationGainMeters.toInt()}", "m", "Dénivelé positif : ${state.metrics.elevationGainMeters.toInt()} mètres")
        }
    }
}

@Composable
private fun MetricLine(icon: ImageVector, tint: Color, value: String, unit: String, spoken: String) {
    Row(Modifier.clearAndSetSemantics { contentDescription = spoken }, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (unit.isEmpty()) value else "$value $unit",
            style = RideakTheme.typography.metric.copy(fontSize = 22.sp, lineHeight = 26.sp),
            color = RideakTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun Controls(
    paused: Boolean,
    spotCount: Int,
    vertical: Boolean,
    viewModel: RideViewModel,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = RideakTheme.colors
    val buttons: @Composable () -> Unit = {
        if (paused) {
            RoundAction(RideakIcons.Stop, "Terminer la sortie", colors.surface, colors.action, 72.dp, outline = colors.action, onClick = onStop)
            RoundAction(RideakIcons.Play, "Reprendre la sortie", colors.nature, colors.onAccent, 96.dp, outline = colors.night, onClick = onResume)
        } else {
            Spacer(Modifier.size(72.dp))
            RoundAction(RideakIcons.Pause, "Mettre en pause", colors.action, Color.White, 96.dp, outline = colors.night) { viewModel.pause() }
        }
        RoundAction(RideakIcons.Heart, "J'aime cet endroit ($spotCount spots dans cette sortie)", colors.surface, colors.action, 72.dp) {
            viewModel.dropSpot()
        }
    }
    if (vertical) {
        Column(
            Modifier.fillMaxHeight().padding(RideakTheme.spacing.m),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { buttons() }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { buttons() }
    }
}

@Composable
private fun FeedbackPill(text: String) {
    val colors = RideakTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = colors.mangaWhite,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .background(colors.night.copy(alpha = 0.9f), RideakTheme.shapes.pill)
            .border(2.dp, colors.action, RideakTheme.shapes.pill)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Header band whose bottom edge rises to the right, as in the mockup. */
private class SlantedBottomShape(private val slant: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { slant.toPx() }
        return Outline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - s)
                lineTo(0f, size.height)
                close()
            },
        )
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
            .background(RideakTheme.colors.night.copy(alpha = 0.7f), RideakTheme.shapes.pill)
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
        modifier = Modifier.size(size).let { if (outline != null) it.border(4.dp, outline, RideakTheme.shapes.pill) else it },
        shape = RideakTheme.shapes.pill,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container, contentColor = content),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(size * 0.4f))
    }
}

@Composable
private fun PermissionGate(permission: LocationPermissionController, onAcceptApproximate: () -> Unit, onCancel: () -> Unit) {
    val colors = RideakTheme.colors
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(RideakTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m, Alignment.CenterVertically),
    ) {
        Text("On a besoin de ta position", style = RideakTheme.typography.impact.copy(fontSize = 38.sp, lineHeight = 40.sp), color = colors.mangaWhite)
        Text(
            "RIDEAK enregistre ton trajet pendant la sortie pour mesurer ta distance et dévoiler ton monde. " +
                "Ta position reste sur ton téléphone.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
        )
        if (!permission.servicesEnabled) {
            Text("La localisation de ton téléphone est coupée.", style = MaterialTheme.typography.bodyLarge, color = colors.reward)
            AdventureButton("ACTIVER LA LOCALISATION", onClick = permission.openLocationSettings)
        }
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
