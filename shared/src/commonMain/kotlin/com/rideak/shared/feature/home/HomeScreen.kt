package com.rideak.shared.feature.home

import androidx.compose.foundation.Image
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import com.rideak.shared.resources.Res
import com.rideak.shared.resources.category_cities
import com.rideak.shared.resources.category_hills
import com.rideak.shared.resources.category_parks
import com.rideak.shared.resources.category_rivers
import com.rideak.shared.resources.hero_home
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.components.TerritoryCard
import com.rideak.shared.format.formatKilometersShort
import com.rideak.shared.designsystem.icons.RideakIcons

/** Home, laid out after mockup "2. HOME". */
@Composable
fun HomeScreen(onRide: () -> Unit, viewModel: HomeViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = RideakTheme.spacing
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Hero()
        Column(
            // The search field overlaps the faded bottom of the illustration, as in the mockup.
            modifier = Modifier.padding(horizontal = spacing.m).offset(y = (-36).dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField()
            CategoryRow()
            TerritoryCard(state.exploredCells)
            StatsRow(state)
            val active = state.activeRide
            AdventureButton(
                text = when {
                    active == null -> "PARTIR\nÀ L'AVENTURE"
                    active.recovered -> "SORTIE\nINTERROMPUE"
                    else -> "RETOUR\nÀ LA SORTIE"
                },
                icon = RideakIcons.Bike,
                minHeight = 64.dp,
                onClick = onRide,
            )
        }
    }
}

@Composable
private fun Hero() {
    // Drawn under the status bar (edge-to-edge); the greeting keeps clear of it.
    Box(Modifier.fillMaxWidth().height(400.dp)) {
        // Illustration generated from the board's Home hero (prompt in docs/assets/hero_home.prompt.txt).
        Image(
            painter = painterResource(Res.drawable.hero_home),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // Sky + title at the top, city in the middle, the cyclist bottom-left (mockup framing).
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = 0.2f),
            modifier = Modifier.fillMaxSize(),
        )
        // Fade into the Night UI below.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0.7f to Color.Transparent, 1f to RideakTheme.colors.night),
            ),
        )
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
        // Illustrated icons generated from the board (prompt in docs/assets/category_icons.prompt.txt).
        Category("Parcs", Res.drawable.category_parks, c.nature)
        Category("Rivières", Res.drawable.category_rivers, c.exploration)
        Category("Villes", Res.drawable.category_cities, c.action)
        Category("Collines", Res.drawable.category_hills, c.reward)
    }
}

@Composable
private fun Category(label: String, icon: DrawableResource, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(56.dp)
                .background(RideakTheme.colors.surface, RideakTheme.shapes.pill)
                .border(1.5.dp, color.copy(alpha = 0.45f), RideakTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(36.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = RideakTheme.colors.textPrimary)
    }
}

@Composable
private fun StatsRow(state: HomeUiState) {
    val c = RideakTheme.colors
    val km = formatKilometersShort(state.totalDistanceMeters)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Stat(km, "km parcourus", c.textPrimary, "$km kilomètres parcourus", Modifier.weight(1f))
        StatDivider()
        Stat("${state.rideCount}", "sorties", c.nature, "${state.rideCount} sorties", Modifier.weight(1f))
        StatDivider()
        Stat("${state.spotCount}", "spots ❤️", c.action, "${state.spotCount} spots favoris", Modifier.weight(1f))
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
