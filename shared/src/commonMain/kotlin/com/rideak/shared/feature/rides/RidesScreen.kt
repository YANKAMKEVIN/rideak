package com.rideak.shared.feature.rides

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.data.recorder.RecorderState
import com.rideak.exploration.GeoPoint
import com.rideak.ride.RideSummary
import kotlin.math.PI
import kotlin.math.cos
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.icons.RideakIcons
import com.rideak.shared.feature.ride.rideButtonLabel
import com.rideak.shared.format.formatDurationShort
import com.rideak.shared.format.formatKilometers
import com.rideak.shared.format.formatKmh
import com.rideak.shared.format.formatRideTitle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RidesScreen(
    onOpenRide: (String) -> Unit,
    onStartRide: () -> Unit = {},
    /** The ride under way, if any: the empty list's button then goes back to it, like the home button. */
    activeRide: RecorderState.Active? = null,
    viewModel: RidesViewModel = koinViewModel(),
) {
    val months by viewModel.months.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<RideSummary?>(null) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = RideakTheme.spacing.m)) {
        Text(
            "Mes sorties",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = RideakTheme.colors.textPrimary,
            modifier = Modifier.padding(vertical = RideakTheme.spacing.m).semantics { heading() },
        )
        val list = months
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RideakTheme.colors.exploration)
            }
            list.isEmpty() -> EmptyRides(rideButtonLabel(activeRide), onStartRide)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
                list.forEach { month ->
                    item(key = month.title) { MonthHeader(month) }
                    items(month.rides, key = { it.ride.id.value }) { item ->
                        RideRow(item, onClick = { onOpenRide(item.ride.id.value) }, onLongClick = { toDelete = item.ride })
                    }
                }
            }
        }
    }
    toDelete?.let { ride ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Supprimer cette sortie ?") },
            text = { Text("${formatRideTitle(ride.startedAtEpochMillis)} (${formatKilometers(ride.distanceMeters)} km), sa trace et ses spots seront supprimés de ce téléphone. Les zones qu'elle seule avait dévoilées retourneront dans le brouillard.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(ride); toDelete = null }) { Text("Supprimer", color = RideakTheme.colors.action) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annuler") } },
        )
    }
}

/** "Octobre 2026 · 42,3 km". */
@Composable
private fun MonthHeader(month: RideMonth) {
    val c = RideakTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(top = RideakTheme.spacing.s).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(month.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text("${formatKilometers(month.totalMeters)} km", style = MaterialTheme.typography.labelLarge, color = c.nature)
    }
}

/** First launch: say what will appear here and offer to start right away instead of a lone line of text. */
@Composable
private fun EmptyRides(buttonLabel: String, onStartRide: () -> Unit) {
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
        AdventureButton(buttonLabel, onClick = onStartRide, icon = RideakIcons.Bike, minHeight = 64.dp)
    }
}

@Composable
private fun RideRow(item: RideListItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = RideakTheme.colors
    val ride = item.ride
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clip(RideakTheme.shapes.medium)
            .background(c.surface)
            .combinedClickable(onClickLabel = "Voir la sortie", onLongClickLabel = "Supprimer la sortie", onLongClick = onLongClick, onClick = onClick)
            .padding(RideakTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrackThumbnail(item.outline)
        Column(Modifier.weight(1f)) {
            Text(formatRideTitle(ride.startedAtEpochMillis), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(
                "${formatDurationShort(ride.activeDurationMillis)} · ${formatKmh(ride.averageMovingSpeedMps)} km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            if (item.revealedCells > 0) {
                Text(
                    "+${item.revealedCells} ${if (item.revealedCells == 1) "zone" else "zones"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.onAccent,
                    modifier = Modifier.padding(top = 4.dp).background(c.reward, RideakTheme.shapes.pill).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text("${formatKilometers(ride.distanceMeters)} km", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = c.nature)
    }
}

/** The shape of the ride, drawn in its square: rides are recognised at a glance. */
@Composable
private fun TrackThumbnail(outline: List<GeoPoint>) {
    val c = RideakTheme.colors
    Canvas(Modifier.size(52.dp).clip(RideakTheme.shapes.small).background(c.surfaceRaised)) {
        if (outline.size < 2) return@Canvas
        // Longitude degrees shrink with latitude: scale them so the shape is not stretched.
        val k = cos(outline.first().latitude * PI / 180)
        val xs = outline.map { it.longitude * k }
        val ys = outline.map { it.latitude }
        val span = maxOf(xs.max() - xs.min(), ys.max() - ys.min()).takeIf { it > 0 } ?: return@Canvas
        val inset = 6.dp.toPx()
        val scale = (size.minDimension - 2 * inset) / span
        val dx = (size.width - (xs.max() - xs.min()) * scale) / 2
        val dy = (size.height - (ys.max() - ys.min()) * scale) / 2
        val path = Path()
        outline.indices.forEach { i ->
            val x = (dx + (xs[i] - xs.min()) * scale).toFloat()
            val y = (size.height - dy - (ys[i] - ys.min()) * scale).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, c.action, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
