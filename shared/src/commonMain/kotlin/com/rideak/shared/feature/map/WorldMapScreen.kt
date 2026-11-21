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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.anchor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
import com.rideak.shared.resources.pin_city
import com.rideak.shared.resources.pin_heart
import com.rideak.shared.resources.pin_hills
import com.rideak.shared.resources.pin_park
import com.rideak.shared.resources.pin_river
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
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

/** One carousel card: an illustration, what has been found in that category, and the filter it applies. */
private data class CategoryCard(val label: String, val image: DrawableResource, val filter: MapFilter, val count: (WorldMapUiState) -> String)

private fun WorldMapUiState.placesOf(vararg categories: PlaceCategory): String {
    val counters = categories.mapNotNull { places[it] }
    return "${counters.sumOf { it.found }} / ${counters.sumOf { it.total }}"
}

private val categories = listOf(
    CategoryCard("Tout", Res.drawable.card_all, MapFilter.ALL) { "${it.exploredCells} zones" },
    CategoryCard("Villes", Res.drawable.card_cities, MapFilter.CITIES) { "${it.cities.found} / ${it.cities.total}" },
    CategoryCard("Parcs", Res.drawable.card_parks, MapFilter.PARKS) { it.placesOf(PlaceCategory.PARK) },
    CategoryCard("Rivières", Res.drawable.card_rivers, MapFilter.RIVERS) { it.placesOf(PlaceCategory.RIVER, PlaceCategory.LAKE) },
    CategoryCard("Forêts", Res.drawable.card_forests, MapFilter.FORESTS) { it.placesOf(PlaceCategory.FOREST) },
)

/** "Ma carte", laid out after mockup "6. MA CARTE". */
@Composable
fun WorldMapScreen(viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val focus = koinInject<MapFocus>()
    val focusedPlace by focus.place.collectAsStateWithLifecycle()
    // Opened on a place from "Lieux": back to a tab where places are pinned.
    LaunchedEffect(focusedPlace) {
        if (focusedPlace != null && MapTab.entries[tab] == MapTab.Rides) tab = MapTab.Exploration.ordinal
    }
    val colors = RideakTheme.colors
    Column(Modifier.fillMaxSize().background(colors.night).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ma carte",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() },
            )
            if (state.hasPrivacyZones) ShareModeToggle(state.shareMode, viewModel::setShareMode)
        }
        Tabs(selected = tab, onSelect = { tab = it })
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp)) {
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                ExplorationMap(state, MapTab.entries[tab], focusedPlace, onFocused = focus::consume)
                state.emptyFilterHint?.let { EmptyFilterHint(it, Modifier.align(Alignment.TopCenter).padding(12.dp)) }
                if (MapTab.entries[tab] == MapTab.Exploration) {
                    TerritoryCard(
                        state.exploredCells,
                        territory = state.territory,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                            .background(colors.night.copy(alpha = 0.6f), RideakTheme.shapes.medium),
                    )
                }
            }
        }
        CategoryCarousel(state, onSelect = viewModel::selectFilter)
    }
}

/** A selected card with nothing discovered yet: say so rather than show an unchanged map. */
@Composable
private fun EmptyFilterHint(text: String, modifier: Modifier = Modifier) {
    val colors = RideakTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(colors.night.copy(alpha = 0.8f), RideakTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
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
private fun ExplorationMap(state: WorldMapUiState, tab: MapTab, focusedPlace: Place?, onFocused: () -> Unit) {
    val colors = RideakTheme.colors
    val heart = painterResource(Res.drawable.pin_heart)
    val pinImages = mapOf(
        PlacePin.PARK to painterResource(Res.drawable.pin_park),
        PlacePin.RIVER to painterResource(Res.drawable.pin_river),
        PlacePin.CITY to painterResource(Res.drawable.pin_city),
        PlacePin.HILLS to painterResource(Res.drawable.pin_hills),
    )
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
        val privacy = rememberGeoJsonSource(GeoJsonData.JsonString(state.privacyZones))
        val outlines = rememberGeoJsonSource(GeoJsonData.JsonString(state.regionOutlines))
        val pinSources = PlacePin.entries.associateWith { pin ->
            rememberGeoJsonSource(GeoJsonData.JsonString(state.placePins[pin] ?: GeoJson.points(emptyList())))
        }

        // Exploration: the unexplored world under a veil, ridden cells clear ("ton monde se dévoile").
        FillLayer(id = "rideak-fog", source = fog, color = const(colors.night), opacity = const(0.42f), visible = tab == MapTab.Exploration)
        // Privacy zones: a discreet veil, for the rider only (empty in share mode).
        FillLayer(id = "rideak-privacy", source = privacy, color = const(colors.lightSurface), opacity = const(0.22f))
        LineLayer(id = "rideak-privacy-edge", source = privacy, color = const(colors.lightSurface), width = const(1.5.dp), opacity = const(0.9f))
        LineLayer(
            id = "rideak-regions", source = outlines, color = const(colors.reward), width = const(2.5.dp), opacity = const(0.85f),
            join = const(LineJoin.Round), visible = tab == MapTab.Exploration,
        )
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
        // Discovered places, one illustrated pin per kind; hidden on the "Mes trajets" tab like the spots.
        PlacePin.entries.forEach { pin ->
            SymbolLayer(
                id = "rideak-places-${pin.name.lowercase()}", source = pinSources.getValue(pin),
                iconImage = image(pinImages.getValue(pin), size = DpSize(36.dp, 36.dp)),
                iconAnchor = const(SymbolAnchor.Bottom),
                iconAllowOverlap = const(true),
                visible = tab != MapTab.Rides,
            )
        }
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
    // After the fit above, so a place opened from "Lieux" wins: centred, close enough to see its streets.
    LaunchedEffect(focusedPlace) {
        val place = focusedPlace ?: return@LaunchedEffect
        val anchor = place.anchor
        mapState.setCameraPosition(CameraPosition(center = Position(longitude = anchor.longitude, latitude = anchor.latitude), zoom = 16.0))
        onFocused()
    }
    MaplibreMap(
        modifier = Modifier.fillMaxSize(),
        state = mapState,
        viewportInsets = PaddingValues(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 130.dp),
    )
}

@Composable
private fun CategoryCarousel(state: WorldMapUiState, onSelect: (MapFilter) -> Unit) {
    // Each card filters the discovered places on the map and frames them; "Tout" shows everything.
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(categories.size) { index ->
            val card = categories[index]
            val count = card.count(state)
            val colors = RideakTheme.colors
            val selected = state.filter == card.filter
            Column(
                Modifier.width(76.dp)
                    .clip(RideakTheme.shapes.medium)
                    .border(if (selected) 2.dp else 0.dp, if (selected) colors.reward else Color.Transparent, RideakTheme.shapes.medium)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(card.filter) })
                    .clearAndSetSemantics {
                        contentDescription = "${card.label} : $count"
                        this.selected = selected
                        role = Role.Tab
                    },
            ) {
                Box(Modifier.height(78.dp).fillMaxWidth()) {
                    Image(painterResource(card.image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to colors.night)))
                }
                Column(
                    Modifier.fillMaxWidth().background(colors.night).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(card.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) colors.reward else colors.textPrimary, maxLines = 1)
                    Text(count, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
                }
            }
        }
    }
}

/** "Mode partage": hides everything inside privacy zones, for screenshots. */
@Composable
private fun ShareModeToggle(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = RideakTheme.colors
    Row(
        Modifier.heightIn(min = 44.dp)
            .clip(RideakTheme.shapes.pill)
            .background(if (enabled) colors.reward else colors.surface)
            .toggleable(enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The fill shows the state; the Switch role announces it to screen readers.
        Text(
            "Mode partage",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.onAccent else colors.textPrimary,
        )
    }
}
