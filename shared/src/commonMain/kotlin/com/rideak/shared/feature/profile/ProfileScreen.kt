package com.rideak.shared.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.feature.map.OfflineMapSection
import com.rideak.shared.feature.map.OfflineMapViewModel
import com.rideak.shared.feature.progress.BadgeGrid
import com.rideak.shared.feature.progress.LevelCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onPrivacyPolicy: () -> Unit = {},
    onCredits: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
    offlineMap: OfflineMapViewModel = koinViewModel(),
) {
    val simulation by viewModel.simulation.collectAsStateWithLifecycle()
    val stress by viewModel.mapStressTest.collectAsStateWithLifecycle()
    val privacy by viewModel.privacyState.collectAsStateWithLifecycle()
    val progress by viewModel.progressState.collectAsStateWithLifecycle()
    val offline by offlineMap.state.collectAsStateWithLifecycle()
    val c = RideakTheme.colors
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(RideakTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m),
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        LevelCard(progress.level)
        BadgeGrid(progress.stats)

        OfflineMapSection(offline, onDownload = offlineMap::download, onDelete = offlineMap::delete)
        PrivacySection(privacy, onProtect = viewModel::protect, onRemove = viewModel::remove)
        AboutSection(onPrivacyPolicy, onCredits, onEraseAll = viewModel::eraseAll)

        if (viewModel.showDeveloperTools) {
            Column(
                Modifier.fillMaxWidth().background(c.surface, RideakTheme.shapes.medium).padding(RideakTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
            ) {
                Text("OUTILS DE DÉVELOPPEMENT", style = MaterialTheme.typography.labelMedium, color = c.reward)
                DevSwitch("Sortie simulée", "Rejoue une boucle de ~3,8 km autour de Cergy à 22 km/h, sans GPS.", simulation, viewModel::setSimulation)
                DevSwitch("Carte : 1 000 zones de test", "Ajoute une exploration synthétique pour mesurer le rendu de la carte.", stress, viewModel::setMapStressTest)
            }
        }
    }
}

@Composable
private fun DevSwitch(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = RideakTheme.colors
    Row(
        Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** Privacy policy, credits and licences, and the right to erasure. */
@Composable
private fun AboutSection(onPrivacyPolicy: () -> Unit, onCredits: () -> Unit, onEraseAll: () -> Unit) {
    val c = RideakTheme.colors
    var confirmErase by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(c.surface, RideakTheme.shapes.medium).padding(vertical = RideakTheme.spacing.s)) {
        TextButton(onClick = onPrivacyPolicy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Politique de confidentialité", color = c.textPrimary, modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onCredits, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Crédits et licences", color = c.textPrimary, modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = { confirmErase = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Effacer toutes mes données", color = c.action, modifier = Modifier.fillMaxWidth())
        }
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Effacer toutes tes données ?") },
            text = { Text("Tes sorties terminées, leurs traces, tes spots ❤️ et tes zones privées seront supprimés de ce téléphone. C'est définitif.") },
            confirmButton = {
                TextButton(onClick = { confirmErase = false; onEraseAll() }) { Text("Tout effacer", color = c.action) }
            },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Annuler") } },
        )
    }
}
