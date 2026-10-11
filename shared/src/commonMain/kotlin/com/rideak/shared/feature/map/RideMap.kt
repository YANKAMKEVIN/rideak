package com.rideak.shared.feature.map

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.SlippyTileGrid
import com.rideak.exploration.TrackPoint
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private val Cergy = GeoPoint(49.0364, 2.0761)

/**
 * One ride on the RIDEAK map: cells it traversed, the track with its glow, spots and, live,
 * the current position. [follow] keeps the camera on the rider; otherwise the camera fits the ride.
 * [insets] keeps MapLibre's attribution and ornaments clear of the UI drawn over the map.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun RideMap(
    segments: List<List<GeoPoint>>,
    modifier: Modifier = Modifier,
    spots: List<GeoPoint> = emptyList(),
    follow: Boolean = false,
    insets: PaddingValues = PaddingValues(0.dp),
) {
    val colors = RideakTheme.colors
    val current = segments.lastOrNull { it.isNotEmpty() }?.last()
    val start = current ?: segments.flatten().firstOrNull() ?: Cergy

    val tracks = remember(segments) { GeoJson.lines(segments) }
    val cells = remember(segments) {
        val grid = SlippyTileGrid()
        GeoJson.squares(ExplorationEngine(grid).cellsFor(segments.flatten().map { TrackPoint(it, 0) }).mapNotNull(grid::boundsOf))
    }
    val spotsJson = remember(spots) { GeoJson.points(spots) }
    val here = remember(current) { GeoJson.points(listOfNotNull(current)) }

    val mapState = rememberMapState(
        baseStyle = BaseStyle.Uri(Res.getUri("files/rideak_style.json")),
        initialCameraPosition = CameraPosition(center = start.toPosition(), zoom = if (follow) 16.0 else 14.0),
    ) {
        val cellSource = rememberGeoJsonSource(GeoJsonData.JsonString(cells))
        val trackSource = rememberGeoJsonSource(GeoJsonData.JsonString(tracks))
        val spotSource = rememberGeoJsonSource(GeoJsonData.JsonString(spotsJson))
        val hereSource = rememberGeoJsonSource(GeoJsonData.JsonString(here))

        LineLayer(id = "ride-cells", source = cellSource, color = const(colors.nature), width = const(1.5.dp), opacity = const(0.7f))
        LineLayer(
            id = "ride-track-glow", source = trackSource, color = const(colors.action), opacity = const(0.35f),
            width = const(14.dp), blur = const(4.dp), cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
        LineLayer(
            id = "ride-track", source = trackSource, color = const(colors.action),
            width = const(5.dp), cap = const(LineCap.Round), join = const(LineJoin.Round),
        )
        CircleLayer(
            id = "ride-spots", source = spotSource, color = const(colors.action), radius = const(7.dp),
            strokeColor = const(Color.White), strokeWidth = const(2.dp),
        )
        if (follow) {
            CircleLayer(
                id = "ride-here", source = hereSource, color = const(colors.exploration), radius = const(9.dp),
                strokeColor = const(Color.White), strokeWidth = const(3.dp),
            )
        }
    }

    if (follow) {
        LaunchedEffect(current) {
            current?.let { mapState.animateCamera(CameraUpdate(center = it.toPosition())) }
        }
    } else {
        LaunchedEffect(segments, spots) {
            val all = (segments.flatten() + spots).map { it.toPosition() }
            if (all.size > 1) mapState.setCameraPosition(mapState.cameraForCoordinates(all))
        }
    }

    MaplibreMap(modifier = modifier, state = mapState, viewportInsets = insets)
}

private fun GeoPoint.toPosition() = Position(longitude = longitude, latitude = latitude)
