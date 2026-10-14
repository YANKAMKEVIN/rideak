package com.rideak.shared.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.format.formatBytes

/** "Carte hors ligne": download the region's base map to ride without network. */
@Composable
fun OfflineMapSection(state: OfflineMapUiState, onDownload: () -> Unit, onDelete: () -> Unit) {
    val name = state.regionName ?: return
    val c = RideakTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, RideakTheme.shapes.medium).padding(RideakTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
    ) {
        Text("Carte hors ligne", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        when (val map = state.map) {
            OfflineMapState.Unavailable -> Text("Préparation…", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            OfflineMapState.NotDownloaded -> {
                Text(
                    "Télécharge le fond de carte de $name (environ 45 Mo, de préférence en Wi-Fi) pour rouler sans réseau. Tes sorties s'enregistrent toujours, même sans carte.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                OutlinedButton(onClick = onDownload, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("Télécharger $name", color = c.textPrimary)
                }
            }
            is OfflineMapState.Downloading -> {
                Text("Téléchargement de $name… ${formatBytes(map.bytes)}", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                if (map.fraction != null) {
                    LinearProgressIndicator(progress = { map.fraction }, modifier = Modifier.fillMaxWidth(), color = c.exploration, trackColor = c.surfaceRaised)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = c.exploration, trackColor = c.surfaceRaised)
                }
            }
            is OfflineMapState.Ready -> {
                Text("$name est disponible hors ligne · ${formatBytes(map.bytes)}", style = MaterialTheme.typography.bodyLarge, color = c.nature)
                TextButton(onClick = onDelete, modifier = Modifier.heightIn(min = 44.dp)) { Text("Supprimer", color = c.action) }
            }
            is OfflineMapState.Failed -> {
                Text("Le téléchargement a échoué : ${map.message}", style = MaterialTheme.typography.bodyMedium, color = c.action)
                OutlinedButton(onClick = onDownload, modifier = Modifier.heightIn(min = 44.dp)) { Text("Réessayer", color = c.textPrimary) }
            }
        }
    }
}
