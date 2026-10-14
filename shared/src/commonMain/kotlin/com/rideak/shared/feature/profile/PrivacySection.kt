package com.rideak.shared.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rideak.exploration.PrivacySuggestion
import com.rideak.exploration.PrivacyZone
import com.rideak.shared.designsystem.RideakTheme
import kotlin.math.roundToInt

private val labels = listOf("Domicile", "Travail", "Autre")

/** Privacy zones: protected places, and frequent starts/ends offered for protection. */
@Composable
fun PrivacySection(
    state: PrivacyUiState,
    onProtect: (PrivacySuggestion, String) -> Unit,
    onRemove: (PrivacyZone) -> Unit,
) {
    val c = RideakTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.surface, RideakTheme.shapes.medium).padding(RideakTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
    ) {
        Text("Zones privées", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        Text(
            "Tout ce qui s'y trouve est masqué en mode partage. Le centre est décalé au hasard : la zone ne trahit pas ton adresse exacte.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        state.zones.forEach { zone ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${zone.label} · ${(zone.radiusMeters / 10).roundToInt() * 10} m",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onRemove(zone) }, modifier = Modifier.heightIn(min = 44.dp)) {
                    Text("Retirer", color = c.action)
                }
            }
        }
        state.suggestions.forEach { suggestion ->
            Column(
                Modifier.fillMaxWidth().background(c.surfaceRaised, RideakTheme.shapes.medium).padding(RideakTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
            ) {
                Text(
                    "${suggestion.rides} sorties partent ou arrivent au même endroit. Le protéger ?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textPrimary,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { label ->
                        OutlinedButton(
                            onClick = { onProtect(suggestion, label) },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text(label, color = c.textPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }
        if (state.zones.isEmpty() && state.suggestions.isEmpty()) {
            Text(
                "Roule un peu : RIDEAK te proposera de protéger les lieux d'où tu pars souvent.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
    }
}
