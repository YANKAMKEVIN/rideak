package com.rideak.shared.feature.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.TerritoryCard
import com.rideak.shared.resources.Res
import com.rideak.shared.resources.card_all
import com.rideak.shared.resources.card_cities
import com.rideak.shared.resources.card_forests
import com.rideak.shared.resources.card_parks
import com.rideak.shared.resources.card_rivers
import com.rideak.shared.resources.pin_heart
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private enum class MapTab(val label: String) { Exploration("Exploration"), Favorites("Lieux favoris"), Rides("Mes trajets") }

private data class CategoryCard(val label: String, val image: DrawableResource, val available: Boolean)

// Per-category exploration needs place data (OSM): only "Tout" is computed for now, the others say so.
private val categories = listOf(
    CategoryCard("Tout", Res.drawable.card_all, available = true),
    CategoryCard("Villes", Res.drawable.card_cities, available = false),
    CategoryCard("Parcs", Res.drawable.card_parks, available = false),
    CategoryCard("Rivières", Res.drawable.card_rivers, available = false),
    CategoryCard("Forêts", Res.drawable.card_forests, available = false),
)

/** "Ma carte", laid out after mockup "6. MA CARTE". */
@Composable
fun WorldMapScreen(viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val colors = RideakTheme.colors
    Column(Modifier.fillMaxSize().background(colors.night).windowInsetsPadding(WindowInsets.statusBars)) {
        Text(
            "Ma carte",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
        )
        Tabs(selected = tab, onSelect = { tab = it })
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp)) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                ExplorationMap(state, MapTab.entries[tab])
                if (MapTab.entries[tab] == MapTab.Exploration) {
                    TerritoryCard(
                        state.exploredCells,
                        Modifier.align(Alignment.BottomCenter).padding(12.dp)
                            .background(colors.night.copy(alpha = 0.6f), RideakTheme.shapes.medium),
                    )
                }
            }
        }
        CategoryCarousel(state.exploredCells)
    }
}

@Composable
private fun Tabs(selected: Int, onSelect: (Int) -> Unit) {
    val colors = RideakTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MapTab.entries.forEachIndexed { index, tab ->
            val isSelected = index == selected
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp)
                    .clip(RideakTheme.shapes.medium)
                    .background(if (isSelected) colors.exploration else colors.surface)
                    .border(1.dp, if (isSelected) colors.exploration else colors.lightSurface.copy(alpha = 0.25f), RideakTheme.shapes.medium)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    // Night on blue (5.5:1) rather than white on blue (3.35:1): small text must pass AA.
                    color = if (isSelected) colors.onAccent else colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun ExplorationMap(state: WorldMapUiState, tab: MapTab) {
    val colors = RideakTheme.colors
    val heart = painterResource(Res.drawable.pin_heart)
    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(Res.getUri("files/rideak_style.json")),
        initialCameraPosition = CameraPosition(
            center = Position(longitude = state.camera.longitude, latitude = state.camera.latitude),
            zoom = state.camera.zoom,
        ),
    ) {
        val fog = rememberGeoJsonSource(GeoJsonData.JsonString(state.fog))
        val cells = rememberGeoJsonSource(GeoJsonData.JsonString(state.cells))
        val tracks = rememberGeoJsonSource(GeoJsonData.JsonString(state.tracks))
        val spots = rememberGeoJsonSource(GeoJsonData.JsonString(state.spots))

        // Exploration: the unexplored world under a veil, ridden cells clear ("ton monde se dévoile").
        FillLayer(id = "rideak-fog", source = fog, color = const(colors.night), opacity = const(0.42f), visible = tab == MapTab.Exploration)
        LineLayer(id = "rideak-cells", source = cells, color = const(colors.nature), width = const(1.5.dp), opacity = const(0.8f), visible = tab == MapTab.Exploration)
        LineLayer(
            id = "rideak-track-glow", source = tracks, color = const(colors.action), opacity = const(0.35f),
            width = const(14.dp), blur = const(4.dp), cap = const(LineCap.Round), join = const(LineJoin.Round),
            visible = tab == MapTab.Rides,
        )
        LineLayer(
            id = "rideak-track", source = tracks, color = const(colors.action),
            opacity = const(if (tab == MapTab.Rides) 1f else 0.55f),
            width = const(if (tab == MapTab.Rides) 5.dp else 3.dp), cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
        SymbolLayer(
            id = "rideak-spots", source = spots,
            iconImage = image(heart, size = DpSize(34.dp, 34.dp)),
            iconAnchor = const(SymbolAnchor.Bottom),
            iconAllowOverlap = const(true),
            visible = tab != MapTab.Rides,
        )
    }
    // Fit everything ridden, keeping it clear of the territory card drawn over the map.
    LaunchedEffect(state.bounds) {
        if (state.bounds.size == 2) {
            mapState.setCameraPosition(
                mapState.cameraForCoordinates(state.bounds.map { Position(longitude = it.longitude, latitude = it.latitude) }),
            )
        }
    }
    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        state = mapState,
        viewportInsets = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 130.dp),
    )
}

@Composable
private fun CategoryCarousel(exploredCells: Int) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories.size) { index ->
            val card = categories[index]
            val isSelected = index == selected
            val colors = RideakTheme.colors
            Column(
                Modifier.width(76.dp)
                    .clip(RideakTheme.shapes.medium)
                    .border(2.dp, if (isSelected) colors.reward else Color.Transparent, RideakTheme.shapes.medium)
                    .selectable(selected = isSelected, enabled = card.available, role = Role.Tab, onClick = { selected = index })
                    .alpha(if (card.available) 1f else 0.6f),
            ) {
                Box(Modifier.height(78.dp).fillMaxWidth()) {
                    Image(painterResource(card.image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.night)))
                }
                Column(
                    Modifier.fillMaxWidth().background(colors.night).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(card.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1)
                    Text(
                        if (card.available) "$exploredCells zones" else "Bientôt",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
