package com.rideak.shared.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.data.PrivacyZoneRepository
import com.rideak.exploration.CellId
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.PrivacyMask
import com.rideak.exploration.PrivacyZone
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.SlippyTileGrid
import com.rideak.exploration.TrackPoint
import com.rideak.shared.di.DeveloperSettings
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.TerritoryProgress
import com.rideak.shared.feature.territory.regions
import com.rideak.shared.feature.territory.territoryOf
import com.rideak.exploration.ExplorationRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ln
import kotlin.math.max

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)

data class WorldMapUiState(
    val loading: Boolean = true,
    val exploredCells: Int = 0,
    val territory: TerritoryProgress? = null,
    /** Outline of every known region, so the rider sees the territory to conquer. */
    val regionOutlines: String = GeoJson.lines(emptyList()),
    val tracks: String = GeoJson.lines(emptyList()),
    val cells: String = GeoJson.squares(emptyList()),
    val fog: String = GeoJson.fog(emptyList()),
    val spots: String = GeoJson.points(emptyList()),
    /** The rider's privacy zones, drawn for them only (never in share mode). */
    val privacyZones: String = GeoJson.circles(emptyList()),
    val hasPrivacyZones: Boolean = false,
    /** Share mode: everything inside a privacy zone is hidden, ready for a screenshot. */
    val shareMode: Boolean = false,
    val camera: MapCamera = MapCamera(49.0364, 2.0761, 12.5),
    /** South-west and north-east corners of everything ridden, for an inset-aware camera fit. */
    val bounds: List<GeoPoint> = emptyList(),
)

/** Everything ridden so far, turned into map overlays. Exploration is derived from stored points, as everywhere. */
class MapViewModel(
    private val repository: RideRepository,
    private val developer: DeveloperSettings,
    private val regions: RegionSource,
    privacy: PrivacyZoneRepository,
) : ViewModel() {
    private class Ridden(
        val segments: List<List<GeoPoint>>,
        val spots: List<GeoPoint>,
        val explored: Set<CellId>,
        val regions: List<ExplorationRegion>,
    )

    private val grid = SlippyTileGrid()
    private val ridden = MutableStateFlow<Ridden?>(null)
    private val shareMode = MutableStateFlow(false)

    val state: StateFlow<WorldMapUiState> =
        combine(ridden.filterNotNull(), privacy.observeZones(), shareMode) { ridden, zones, share ->
            withContext(Dispatchers.Default) { build(ridden, zones, share) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorldMapUiState())

    init {
        viewModelScope.launch {
            val track = repository.finishedSegments() +
                if (developer.mapStressTest.value) listOf(stressTrack()) else emptyList()
            val spots = repository.finishedSpots().map { it.position }
            val regions = regions.regions()
            ridden.value = withContext(Dispatchers.Default) {
                Ridden(
                    segments = track.map { segment -> segment.map { it.position } },
                    spots = spots,
                    explored = ExplorationEngine.standard(grid).cellsForSegments(track),
                    regions = regions,
                )
            }
        }
    }

    fun setShareMode(enabled: Boolean) {
        shareMode.value = enabled
    }

    private fun build(ridden: Ridden, zones: List<PrivacyZone>, share: Boolean): WorldMapUiState {
        // The rider sees everything on their own map; share mode hides what lies in their zones.
        val mask = PrivacyMask(if (share) zones else emptyList())
        val cells = mask.cells(ridden.explored, grid).mapNotNull(grid::boundsOf)
        val all = ridden.segments.flatten() + ridden.spots
        return WorldMapUiState(
            loading = false,
            exploredCells = ridden.explored.size,
            territory = territoryOf(ridden.regions, ridden.explored),
            regionOutlines = GeoJson.lines(
                ridden.regions.flatMap { region -> region.boundary.flatMap { listOf(it.exterior) + it.holes } }.map { it + it.first() },
            ),
            tracks = GeoJson.lines(mask.lines(ridden.segments)),
            cells = GeoJson.squares(cells),
            fog = GeoJson.fog(cells),
            spots = GeoJson.points(mask.points(ridden.spots)),
            privacyZones = GeoJson.circles(if (share) emptyList() else zones),
            hasPrivacyZones = zones.isNotEmpty(),
            shareMode = share,
            camera = if (all.isEmpty()) WorldMapUiState().camera else fit(all),
            bounds = if (all.isEmpty()) emptyList() else listOf(
                GeoPoint(all.minOf { it.latitude }, all.minOf { it.longitude }),
                GeoPoint(all.maxOf { it.latitude }, all.maxOf { it.longitude }),
            ),
        )
    }

    /** Debug: a ~220 km random ride around Cergy (5 m/s, one fix per second) touching about a thousand cells. */
    private fun stressTrack(): List<TrackPoint> {
        val random = kotlin.random.Random(7)
        var lat = 49.0364; var lon = 2.0761; var heading = 0.0
        return List(45_000) {
            heading += random.nextDouble(-0.12, 0.12)
            lat += 0.000045 * kotlin.math.cos(heading); lon += 0.000068 * kotlin.math.sin(heading)
            TrackPoint(GeoPoint(lat, lon), it * 1_000L)
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
