package com.rideak.shared.feature.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.ride.RideSummary
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.icons.RideakIcons
import com.rideak.shared.format.formatDurationShort
import com.rideak.shared.format.formatKilometers
import com.rideak.shared.format.formatKmh
import com.rideak.shared.format.formatRideTitle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RidesScreen(onOpenRide: (String) -> Unit, onStartRide: () -> Unit = {}, viewModel: RidesViewModel = koinViewModel()) {
    val rides by viewModel.rides.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = RideakTheme.spacing.m)) {
        Text(
            "Mes sorties",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = RideakTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = RideakTheme.spacing.m).semantics { heading() },
        )
        val list = rides
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RideakTheme.colors.exploration)
            }
            list.isEmpty() -> EmptyRides(onStartRide)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
                items(list, key = { it.id.value }) { ride -> RideRow(ride) { onOpenRide(ride.id.value) } }
            }
        }
    }
}

/** First launch: say what will appear here and offer to start right away instead of a lone line of text. */
@Composable
private fun EmptyRides(onStartRide: () -> Unit) {
    val c = RideakTheme.colors
    Column(
        Modifier.fillMaxSize().padding(bottom = RideakTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(RideakTheme.shapes.pill).background(Brush.linearGradient(listOf(c.exploration, c.nature))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(RideakIcons.Bike, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
        }
        Text(
            "Aucune sortie pour l'instant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            "Chaque sortie dévoile un peu plus ton monde. Elle apparaîtra ici avec sa distance, sa durée et sa vitesse.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        AdventureButton("PARTIR\nÀ L'AVENTURE", onClick = onStartRide, icon = RideakIcons.Bike, minHeight = 64.dp)
    }
}

@Composable
private fun RideRow(ride: RideSummary, onClick: () -> Unit) {
    val c = RideakTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clip(RideakTheme.shapes.medium)
            .background(c.surface)
            .clickable(onClickLabel = "Voir la sortie", onClick = onClick)
            .padding(RideakTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(formatRideTitle(ride.startedAtEpochMillis), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(
                "${formatDurationShort(ride.activeDurationMillis)} · ${formatKmh(ride.averageMovingSpeedMps)} km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
        Text("${formatKilometers(ride.distanceMeters)} km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = c.nature)
    }
}
