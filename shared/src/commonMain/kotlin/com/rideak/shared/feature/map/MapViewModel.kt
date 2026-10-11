package com.rideak.shared.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.SlippyTileGrid
import com.rideak.exploration.TrackPoint
import com.rideak.shared.di.DeveloperSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.max

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)

data class WorldMapUiState(
    val loading: Boolean = true,
    val exploredCells: Int = 0,
    val tracks: String = GeoJson.lines(emptyList()),
    val cells: String = GeoJson.squares(emptyList()),
    val fog: String = GeoJson.fog(emptyList()),
    val spots: String = GeoJson.points(emptyList()),
    val camera: MapCamera = MapCamera(49.0364, 2.0761, 12.5),
    /** South-west and north-east corners of everything ridden, for an inset-aware camera fit. */
    val bounds: List<GeoPoint> = emptyList(),
)

/** Everything ridden so far, turned into map overlays. Exploration is derived from stored points, as everywhere. */
class MapViewModel(
    private val repository: RideRepository,
    private val developer: DeveloperSettings,
) : ViewModel() {
    private val _state = MutableStateFlow(WorldMapUiState())
    val state: StateFlow<WorldMapUiState> = _state

    init {
        viewModelScope.launch {
            val segments = repository.finishedSegments() +
                if (developer.mapStressTest.value) listOf(stressTrack()) else emptyList()
            val spots = repository.finishedSpots().map { it.position }
            _state.value = withContext(Dispatchers.Default) { build(segments, spots) }
        }
    }

    private fun build(segments: List<List<GeoPoint>>, spots: List<GeoPoint>): WorldMapUiState {
        val grid = SlippyTileGrid()
        val cells = ExplorationEngine(grid)
            .cellsFor(segments.flatten().map { TrackPoint(it, 0) })
            .mapNotNull(grid::boundsOf)
        val all = segments.flatten() + spots
        return WorldMapUiState(
            loading = false,
            exploredCells = cells.size,
            tracks = GeoJson.lines(segments),
            cells = GeoJson.squares(cells),
            fog = GeoJson.fog(cells),
            spots = GeoJson.points(spots),
            camera = if (all.isEmpty()) WorldMapUiState().camera else fit(all),
            bounds = if (all.isEmpty()) emptyList() else listOf(
                GeoPoint(all.minOf { it.latitude }, all.minOf { it.longitude }),
                GeoPoint(all.maxOf { it.latitude }, all.maxOf { it.longitude }),
            ),
        )
    }

    /** Debug: a ~220 km random ride around Cergy touching about a thousand cells. */
    private fun stressTrack(): List<GeoPoint> {
        val random = kotlin.random.Random(7)
        var lat = 49.0364; var lon = 2.0761; var heading = 0.0
        return List(45_000) {
            heading += random.nextDouble(-0.12, 0.12)
            lat += 0.000045 * kotlin.math.cos(heading); lon += 0.000068 * kotlin.math.sin(heading)
            GeoPoint(lat, lon)
        }
    }

    /** Centre on the data with a zoom that keeps it on screen (~400 dp wide phone, some margin). */
    private fun fit(points: List<GeoPoint>): MapCamera {
        val south = points.minOf { it.latitude }; val north = points.maxOf { it.latitude }
        val west = points.minOf { it.longitude }; val east = points.maxOf { it.longitude }
        // At least ~1.5 km of context around a short ride.
        val span = max(max(east - west, (north - south) * 1.5), 0.02)
        val zoom = (ln(360.0 * 400 / 256 / span) / ln(2.0) - 0.6).coerceIn(3.0, 16.5)
        return MapCamera((south + north) / 2, (west + east) / 2, zoom)
    }
}
