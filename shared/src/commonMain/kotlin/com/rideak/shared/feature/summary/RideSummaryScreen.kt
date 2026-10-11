package com.rideak.shared.feature.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.TrackPreview
import com.rideak.shared.designsystem.icons.RideakIcons
import com.rideak.shared.format.formatClock
import com.rideak.shared.format.formatDurationShort
import com.rideak.shared.format.formatKilometers
import com.rideak.shared.format.formatKmh
import com.rideak.shared.format.formatRideTitle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// Purple of the mockup's climb icon; decorative only (never carries text).
private val ClimbPurple = Color(0xFFB45CFF)

/** End of ride, laid out after mockup "5. FIN DE SORTIE". */
@Composable
fun RideSummaryScreen(rideId: String, onBack: () -> Unit) {
    val viewModel: RideSummaryViewModel = koinViewModel(key = rideId) { parametersOf(rideId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ride = state.ride
    Box(Modifier.fillMaxSize().background(RideakTheme.colors.night)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            ride == null -> Text("Sortie introuvable.", color = RideakTheme.colors.textSecondary, modifier = Modifier.align(Alignment.Center))
            else -> Summary(state, onBack)
        }
    }
}

@Composable
private fun Summary(state: RideSummaryUiState, onBack: () -> Unit) {
    val ride = requireNotNull(state.ride)
    val c = RideakTheme.colors
    val spacing = RideakTheme.spacing
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // Header: back, round thumbnail, title + time range.
        Row(Modifier.padding(horizontal = spacing.s, vertical = spacing.s), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = c.textPrimary)
            }
            Box(
                Modifier.size(52.dp).clip(RideakTheme.shapes.pill)
                    .background(Brush.linearGradient(listOf(c.exploration, c.nature))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(RideakIcons.Bike, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
                Text(formatRideTitle(ride.startedAtEpochMillis), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary)
                Text(
                    formatClock(ride.startedAtEpochMillis) + (ride.endedAtEpochMillis?.let { " – " + formatClock(it) } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }

        TrackPreview(
            segments = state.segments,
            spots = state.spots.map { it.position },
            modifier = Modifier.fillMaxWidth().height(260.dp),
            minSpanMeters = 600.0,
        )

        Column(Modifier.padding(spacing.m), verticalArrangement = Arrangement.spacedBy(spacing.m)) {
            if (tab == 0) {
                StatsPanel(state)
                Text(
                    "${formatDurationShort(ride.movingMillis)} en mouvement · ${state.cellsTraversed} zones traversées",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                ElevationChart(state.profile, Modifier.fillMaxWidth())
            } else {
                SpotsList(state)
            }
            Tabs(selected = tab, spotCount = state.spots.size, onSelect = { tab = it })
        }
    }
}

@Composable
private fun StatsPanel(state: RideSummaryUiState) {
    val ride = requireNotNull(state.ride)
    val c = RideakTheme.colors
    val left = listOf(
        Stat(RideakIcons.Stopwatch, c.nature, "${formatKilometers(ride.distanceMeters)} km", "Distance"),
        Stat(RideakIcons.Speedometer, c.reward, "${formatKmh(ride.averageMovingSpeedMps)} km/h", "Moyenne"),
        // Both climb values come from the replay of the stored points (single source of truth).
        Stat(RideakIcons.Mountain, ClimbPurple, "${state.metrics.elevationGainMeters.toInt()} m", "Dénivelé +"),
    )
    val right = listOf(
        Stat(RideakIcons.Timer, c.exploration, formatDurationShort(ride.activeDurationMillis), "Durée"),
        Stat(RideakIcons.Speedometer, c.action, "${formatKmh(ride.maxSpeedMps)} km/h", "Max"),
        Stat(Icons.Filled.LocationOn, c.nature, "${state.metrics.elevationLossMeters.toInt()} m", "Dénivelé -"),
    )
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .background(c.surface, RideakTheme.shapes.medium)
            .padding(vertical = RideakTheme.spacing.m),
    ) {
        StatColumn(left, Modifier.weight(1f))
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.lightSurface.copy(alpha = 0.2f)))
        StatColumn(right, Modifier.weight(1f))
    }
}

private data class Stat(val icon: ImageVector, val tint: Color, val value: String, val label: String)

@Composable
private fun StatColumn(stats: List<Stat>, modifier: Modifier) {
    Column(modifier.padding(horizontal = RideakTheme.spacing.m), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        stats.forEach { stat ->
            Row(
                Modifier.clearAndSetSemantics { contentDescription = "${stat.label} : ${stat.value}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(stat.icon, contentDescription = null, tint = stat.tint, modifier = Modifier.size(42.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stat.value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RideakTheme.colors.textPrimary,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(stat.label, style = MaterialTheme.typography.bodyMedium, color = RideakTheme.colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun SpotsList(state: RideSummaryUiState) {
    val c = RideakTheme.colors
    if (state.spots.isEmpty()) {
        Text("Aucun spot ❤️ pendant cette sortie.", style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
        return
    }
    state.spots.forEachIndexed { i, spot ->
        Row(
            Modifier.fillMaxWidth().background(c.surface, RideakTheme.shapes.medium).padding(RideakTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(RideakIcons.Heart, contentDescription = null, tint = c.action, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text("Spot ${i + 1} · ${formatClock(spot.createdAtEpochMillis)}", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        }
    }
}

/** Pill tabs (mockup: "Aperçu / Lieux / Photos"); Photos arrives with its feature. */
@Composable
private fun Tabs(selected: Int, spotCount: Int, onSelect: (Int) -> Unit) {
    val c = RideakTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.surface, RideakTheme.shapes.pill).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf("Aperçu", "Spots ($spotCount)").forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .clip(RideakTheme.shapes.pill)
                    .then(if (isSelected) Modifier.border(1.5.dp, c.mangaWhite, RideakTheme.shapes.pill) else Modifier)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) c.textPrimary else c.textSecondary,
                )
            }
        }
    }
}
