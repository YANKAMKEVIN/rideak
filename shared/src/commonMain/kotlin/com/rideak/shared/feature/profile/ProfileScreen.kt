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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = koinViewModel()) {
    val simulation by viewModel.simulation.collectAsStateWithLifecycle()
    val stress by viewModel.mapStressTest.collectAsStateWithLifecycle()
    val privacy by viewModel.privacyState.collectAsStateWithLifecycle()
    val c = RideakTheme.colors
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(RideakTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.m),
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        Text("Ta progression arrive bientôt.", style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)

        PrivacySection(privacy, onProtect = viewModel::protect, onRemove = viewModel::remove)

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
