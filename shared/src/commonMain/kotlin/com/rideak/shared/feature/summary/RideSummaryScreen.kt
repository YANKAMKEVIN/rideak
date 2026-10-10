package com.rideak.shared.feature.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.ride.RideSummary
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.format.formatClock
import com.rideak.shared.format.formatDurationShort
import com.rideak.shared.format.formatKilometers
import com.rideak.shared.format.formatKmh
import com.rideak.shared.format.formatRideTitle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** End of ride (mockup "5. FIN DE SORTIE"), without map and chart for now. */
@Composable
fun RideSummaryScreen(rideId: String, onBack: () -> Unit) {
    val viewModel: RideSummaryViewModel = koinViewModel(key = rideId) { parametersOf(rideId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().background(RideakTheme.colors.night)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(RideakTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = RideakTheme.colors.textPrimary)
        }
        val ride = state.ride
        when {
            state.loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ride == null -> Text("Sortie introuvable.", color = RideakTheme.colors.textSecondary)
            else -> Summary(ride, state.cellsTraversed, onBack)
        }
    }
}

@Composable
private fun Summary(ride: RideSummary, cells: Int, onBack: () -> Unit) {
    val c = RideakTheme.colors
    Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
        Text(formatRideTitle(ride.startedAtEpochMillis), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
        val end = ride.endedAtEpochMillis
        Text(
            formatClock(ride.startedAtEpochMillis) + (end?.let { " – " + formatClock(it) } ?: ""),
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
        )
    }
    val tiles = listOf(
        Tile(formatKilometers(ride.distanceMeters), "km", "Distance", c.nature),
        Tile(formatDurationShort(ride.activeDurationMillis), "", "Durée", c.exploration),
        Tile(formatKmh(ride.averageMovingSpeedMps), "km/h", "Moyenne", c.reward),
        Tile(formatKmh(ride.maxSpeedMps), "km/h", "Max", c.action),
        Tile("${ride.elevationGainMeters.toInt()}", "m", "Dénivelé +", c.exploration),
        Tile(formatDurationShort(ride.movingMillis), "", "En mouvement", c.nature),
        Tile("$cells", "", "Zones traversées", c.reward),
        Tile("${ride.spotCount}", "", "Spots ❤️", c.action),
    )
    tiles.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m)) {
            row.forEach { StatTile(it, Modifier.weight(1f)) }
        }
    }
    Text(
        "La carte de ta sortie arrive avec le prochain jalon.",
        style = MaterialTheme.typography.bodyMedium,
        color = c.textSecondary,
    )
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Terminé") }
}

private data class Tile(val value: String, val unit: String, val label: String, val accent: Color)

@Composable
private fun StatTile(tile: Tile, modifier: Modifier) {
    Column(
        modifier
            .background(RideakTheme.colors.surface, RideakTheme.shapes.medium)
            .padding(RideakTheme.spacing.m)
            .clearAndSetSemantics { contentDescription = "${tile.label} : ${tile.value} ${tile.unit}" },
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(tile.value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = tile.accent)
            if (tile.unit.isNotEmpty()) Text(" ${tile.unit}", style = MaterialTheme.typography.titleSmall, color = RideakTheme.colors.textSecondary)
        }
        Text(tile.label, style = MaterialTheme.typography.bodyMedium, color = RideakTheme.colors.textPrimary)
    }
}
