package com.rideak.shared.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.viewmodel.koinViewModel
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * "Ma carte" (mockup "6. MA CARTE"): the real map in the RIDEAK night style, the world fogged
 * except the cells already ridden, every ride's track and the ❤️ spots. MapLibre spike.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun WorldMapScreen(viewModel: MapViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = RideakTheme.colors
    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
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

        // Unexplored world under a night veil; ridden cells stay clear ("ton monde se dévoile").
        FillLayer(id = "rideak-fog", source = fog, color = const(colors.night), opacity = const(0.55f))
        LineLayer(id = "rideak-cells", source = cells, color = const(colors.nature), width = const(1.5.dp), opacity = const(0.7f))
        LineLayer(
            id = "rideak-track-glow", source = tracks, color = const(colors.action), opacity = const(0.35f),
            width = const(12.dp), blur = const(4.dp), cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
        LineLayer(
            id = "rideak-track", source = tracks, color = const(colors.action),
            width = const(4.dp), cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
        CircleLayer(
            id = "rideak-spots", source = spots, color = const(colors.action), radius = const(7.dp),
            strokeColor = const(Color.White), strokeWidth = const(2.dp),
        )
    }

    Box(Modifier.fillMaxSize()) {
        MaplibreMap(modifier = Modifier.fillMaxSize(), state = mapState)
        Column(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(colors.night, colors.night.copy(alpha = 0f))))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            Text(
                "MA CARTE",
                style = RideakTheme.typography.impact.copy(fontSize = 34.sp),
                color = colors.mangaWhite,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                if (state.exploredCells == 0) "Roule pour dissiper le brouillard" else "${state.exploredCells} zones explorées",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
        }
    }
}
