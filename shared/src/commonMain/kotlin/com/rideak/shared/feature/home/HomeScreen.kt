package com.rideak.shared.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rideak.exploration.ExplorationProgress
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.components.ExplorationProgressBar
import com.rideak.shared.designsystem.components.RideMetric
import kotlinx.coroutines.launch

/** J0 home: a living showcase of the design system. All values below are demo data. */
@Composable
fun HomeScreen(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val spacing = RideakTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.m),
        verticalArrangement = Arrangement.spacedBy(spacing.l),
    ) {
        Column {
            Text(
                "Yo ! Prêt à explorer ?",
                style = RideakTheme.typography.impact,
                color = RideakTheme.colors.mangaWhite,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "Plus tu roules, plus ton monde se dévoile.",
                style = MaterialTheme.typography.bodyLarge,
                color = RideakTheme.colors.textSecondary,
            )
        }

        DemoLabel()
        ExplorationProgressBar(
            regionName = "Cergy-Pontoise",
            progress = ExplorationProgress(exploredRideableCells = 12, totalRideableCells = 100),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
            RideMetric("147", "km", "parcourus", spokenUnit = "kilomètres")
            RideMetric("8", "", "lieux découverts")
            RideMetric("4", "", "lieux ❤️", modifier = Modifier.width(96.dp))
        }

        AdventureButton(
            text = "PARTIR À L'AVENTURE",
            onClick = { scope.launch { snackbarHostState.showSnackbar("Le suivi GPS arrive au prochain jalon.") } },
        )

        PaletteShowcase()
    }
}

@Composable
private fun DemoLabel() {
    Text(
        "DONNÉES DE DÉMONSTRATION",
        style = MaterialTheme.typography.labelMedium,
        color = RideakTheme.colors.reward,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteShowcase() {
    val c = RideakTheme.colors
    val swatches = listOf(
        "Exploration" to c.exploration, "Nature" to c.nature, "Action" to c.action, "Récompense" to c.reward,
        "Nuit" to c.night, "Surface" to c.surface, "Gris clair" to c.lightSurface, "Blanc manga" to c.mangaWhite,
    )
    Column(verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s)) {
        Text(
            "Palette v0.1",
            style = MaterialTheme.typography.titleMedium,
            color = c.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(RideakTheme.spacing.s),
        ) {
            swatches.forEach { (name, color) -> Swatch(name, color) }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            // Outlined so Night stays visible on the Night background.
            Modifier.size(56.dp).clip(RideakTheme.shapes.small).background(color)
                .border(1.dp, RideakTheme.colors.lightSurface, RideakTheme.shapes.small),
        )
        Text(name, style = MaterialTheme.typography.labelSmall, color = RideakTheme.colors.textSecondary)
    }
}
