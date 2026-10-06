package com.rideak.shared.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideak.exploration.ExplorationProgress
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.components.ExplorationProgressBar
import com.rideak.shared.designsystem.icons.RideakIcons
import kotlinx.coroutines.launch

// Placeholder values until rides are recorded (J1+). Same numbers as the mockup.
private val demoProgress = ExplorationProgress(exploredRideableCells = 12, totalRideableCells = 100)

/** Home, laid out after mockup "2. HOME". */
@Composable
fun HomeScreen(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val spacing = RideakTheme.spacing
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Hero()
        Column(
            modifier = Modifier.padding(horizontal = spacing.m).offset(y = (-12).dp),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            SearchField()
            CategoryRow()
            ExplorationProgressBar(regionName = "Cergy-Pontoise", progress = demoProgress)
            StatsRow()
            AdventureButton(
                text = "PARTIR\nÀ L'AVENTURE",
                icon = RideakIcons.Bike,
                onClick = { scope.launch { snackbarHostState.showSnackbar("Le suivi GPS arrive au prochain jalon.") } },
            )
        }
    }
}

@Composable
private fun Hero() {
    // Drawn under the status bar (edge-to-edge); the greeting keeps clear of it.
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        HeroLandscape(Modifier.fillMaxSize())
        val outline = Shadow(RideakTheme.colors.night, Offset(3f, 4f), blurRadius = 6f)
        Column(
            Modifier.windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = RideakTheme.spacing.m, vertical = RideakTheme.spacing.s)
                .semantics(mergeDescendants = true) { heading() },
        ) {
            Text(
                "Yo !",
                style = RideakTheme.typography.impact.copy(fontSize = 64.sp, lineHeight = 68.sp, shadow = outline),
                color = RideakTheme.colors.mangaWhite,
            )
            Text(
                "Prêt à explorer ?",
                style = RideakTheme.typography.impact.copy(fontSize = 28.sp, lineHeight = 32.sp, shadow = outline),
                color = RideakTheme.colors.mangaWhite,
            )
        }
    }
}

@Composable
private fun SearchField() {
    // Visual only for now: place search arrives with the map.
    val colors = RideakTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(colors.surface, RideakTheme.shapes.pill)
            .border(1.dp, colors.lightSurface.copy(alpha = 0.3f), RideakTheme.shapes.pill)
            .padding(horizontal = RideakTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = colors.textPrimary)
        Text("Explorer autour de moi", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
    }
}

@Composable
private fun CategoryRow() {
    val c = RideakTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Category("Parcs", RideakIcons.Park, c.nature)
        Category("Rivières", RideakIcons.River, c.exploration)
        Category("Villes", RideakIcons.City, c.action)
        Category("Collines", RideakIcons.Hills, c.reward)
    }
}

@Composable
private fun Category(label: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(64.dp)
                .background(RideakTheme.colors.surface, RideakTheme.shapes.pill)
                .border(1.5.dp, color.copy(alpha = 0.45f), RideakTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = RideakTheme.colors.textPrimary)
    }
}

@Composable
private fun StatsRow() {
    val c = RideakTheme.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Stat("147", "km parcourus", c.textPrimary, "147 kilomètres parcourus", Modifier.weight(1f))
        StatDivider()
        Stat("8", "lieux découverts", c.nature, "8 lieux découverts", Modifier.weight(1f))
        StatDivider()
        Stat("4", "lieux favoris", c.action, "4 lieux favoris", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, spoken: String, modifier: Modifier) {
    Column(
        modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = RideakTheme.colors.textPrimary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 4.dp).background(RideakTheme.colors.lightSurface.copy(alpha = 0.3f)))
}
