package com.rideak.shared.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.PrivacyZoneRepository
import com.rideak.data.RideRepository
import com.rideak.exploration.CellId
import com.rideak.exploration.ExplorationEngine
import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.anchor
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceDiscoverer
import com.rideak.exploration.PrivacyMask
import com.rideak.exploration.PrivacyZone
import com.rideak.exploration.SlippyTileGrid
import com.rideak.exploration.TrackPoint
import com.rideak.shared.di.DeveloperSettings
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.TerritoryProgress
import com.rideak.shared.feature.territory.territoryOf
import kotlin.math.ln
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class MapCamera(val latitude: Double, val longitude: Double, val zoom: Double)

/** A carousel card used as a filter: which discovered places stay on the map. Null categories: all of them. */
enum class MapFilter(val categories: Set<PlaceCategory>?, private val nothingYet: String = "") {
    ALL(null),
    CITIES(setOf(PlaceCategory.MONUMENT, PlaceCategory.LANDMARK), "Aucun monument ni curiosité découvert pour l'instant"),
    PARKS(setOf(PlaceCategory.PARK), "Aucun parc découvert pour l'instant"),
    RIVERS(setOf(PlaceCategory.RIVER, PlaceCategory.LAKE), "Aucune rivière ni plan d'eau découvert pour l'instant"),
    FORESTS(setOf(PlaceCategory.FOREST), "Aucune forêt découverte pour l'instant"),
    ;

    /**
     * What the map says when this card is selected but none of its places is discovered yet, rather
     * than an unchanged map. Null for "Tout", or once one is found (even if share mode hides it).
     */
    fun emptyHint(places: Map<PlaceCategory, Counter>): String? {
        val categories = categories ?: return null
        return if (categories.sumOf { places[it]?.found ?: 0 } == 0) "$nothingYet : pars à leur recherche !" else null
    }

    /** The filter to keep when a place of [category] is opened on the map: this one if it shows it, else "Tout". */
    fun showing(category: PlaceCategory): MapFilter =
        if (categories == null || category in categories) this else ALL
}

/** The four illustrated pins (design/generated/pin_*.webp) and the categories each one marks. */
enum class PlacePin(val categories: Set<PlaceCategory>) {
    PARK(setOf(PlaceCategory.PARK, PlaceCategory.FOREST)),
    RIVER(setOf(PlaceCategory.RIVER, PlaceCategory.LAKE)),
    CITY(setOf(PlaceCategory.MONUMENT, PlaceCategory.LANDMARK)),
    HILLS(setOf(PlaceCategory.VIEWPOINT)),
}

/** "found / total" for the category carousel. */
data class Counter(val found: Int, val total: Int)

data class WorldMapUiState(
    val loading: Boolean = true,
    val exploredCells: Int = 0,
    val territory: TerritoryProgress? = null,
    /** Cities (regions) explored, and places discovered per category, for the carousel. */
    val cities: Counter = Counter(0, 0),
    val places: Map<PlaceCategory, Counter> = emptyMap(),
    /** Outline of every known region, so the rider sees the territory to conquer. */
    val regionOutlines: String = GeoJson.lines(emptyList()),
    val tracks: String = GeoJson.lines(emptyList()),
    val cells: String = GeoJson.squares(emptyList()),
    val fog: String = GeoJson.fog(emptyList()),
    val spots: String = GeoJson.points(emptyList()),
    /** Discovered places kept by [filter], one GeoJSON layer per pin illustration. */
    val placePins: Map<PlacePin, String> = emptyMap(),
    val filter: MapFilter = MapFilter.ALL,
    /** Discovered places kept by [filter] (names for screen readers, positions for the camera). */
    val shownPlaces: List<Place> = emptyList(),
    /** Set when the selected card has nothing discovered yet ([MapFilter.emptyHint]). */
    val emptyFilterHint: String? = null,
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
        val places: Map<PlaceCategory, Counter>,
        val discovered: List<Place>,
    )

    private val grid = SlippyTileGrid()
    private val shareMode = MutableStateFlow(false)
    private val filter = MutableStateFlow(MapFilter.ALL)

    /**
     * Recomputed whenever finished rides change (like Home and Places): the map tab is kept alive,
     * so a ride finished meanwhile must appear and "delete all my data" must clear the tracks.
     * The debug stress ride is followed too, so toggling it shows without leaving the tab.
     */
    private val ridden = combine(repository.observeFinishedRides(), developer.mapStressTest) { _, stress -> loadRidden(stress) }

    val state: StateFlow<WorldMapUiState> =
        combine(ridden, privacy.observeZones(), shareMode, filter) { ridden, zones, share, filter ->
            withContext(Dispatchers.Default) { build(ridden, zones, share, filter) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorldMapUiState())

    private suspend fun loadRidden(stress: Boolean): Ridden {
        val track = repository.finishedSegments() + if (stress) listOf(stressTrack()) else emptyList()
        val spots = repository.finishedSpots().map { it.position }
        val packs = regions.packs()
        return withContext(Dispatchers.Default) {
            val engine = ExplorationEngine.standard(grid)
            val places = packs.flatMap { it.places }
            val found = PlaceDiscoverer().discovered(places, engine.pathsFor(track))
            Ridden(
                segments = track.map { segment -> segment.map { it.position } },
                spots = spots,
                explored = engine.cellsForSegments(track),
                regions = packs.map { it.region },
                places = places.groupBy { it.category }.mapValues { (_, list) -> Counter(list.count { it.id in found }, list.size) },
                discovered = places.filter { it.id in found },
            )
        }
    }

    fun setShareMode(enabled: Boolean) {
        shareMode.value = enabled
    }

    /** Tapping the selected card again shows everything. */
    fun selectFilter(selected: MapFilter) {
        filter.value = if (filter.value == selected) MapFilter.ALL else selected
    }

    /**
     * A place opened from "Lieux" must show its pin: a card that hides its category gives way to "Tout".
     * A card that already shows it stays selected.
     */
    fun reveal(place: Place) {
        filter.value = filter.value.showing(place.category)
    }

    private fun build(ridden: Ridden, zones: List<PrivacyZone>, share: Boolean, filter: MapFilter): WorldMapUiState {
        // The rider sees everything on their own map; share mode hides what lies in their zones.
        val mask = PrivacyMask(if (share) zones else emptyList())
        val cells = mask.cells(ridden.explored, grid).mapNotNull(grid::boundsOf)
        val all = ridden.segments.flatten() + ridden.spots
        // Share mode also hides discovered places touching a privacy zone.
        val shown = ridden.discovered
            .filter { filter.categories == null || it.category in filter.categories }
            .filterNot(mask::hides)
        // A filter frames its places; without one, everything ridden.
        val framed = if (filter != MapFilter.ALL && shown.isNotEmpty()) shown.map { it.anchor } else all
        return WorldMapUiState(
            loading = false,
            exploredCells = ridden.explored.size,
            territory = territoryOf(ridden.regions, ridden.explored),
            cities = Counter(ridden.regions.count { it.progress(ridden.explored).exploredRideableCells > 0 }, ridden.regions.size),
            places = ridden.places,
            regionOutlines = GeoJson.lines(
                ridden.regions.flatMap { region -> region.boundary.flatMap { listOf(it.exterior) + it.holes } }.map { it + it.first() },
            ),
            tracks = GeoJson.lines(mask.lines(ridden.segments)),
            cells = GeoJson.squares(cells),
            fog = GeoJson.fog(cells),
            spots = GeoJson.points(mask.points(ridden.spots)),
            placePins = PlacePin.entries.associateWith { pin -> GeoJson.points(shown.filter { it.category in pin.categories }.map { it.anchor }) },
            filter = filter,
            shownPlaces = shown,
            emptyFilterHint = filter.emptyHint(ridden.places),
            privacyZones = GeoJson.circles(if (share) emptyList() else zones),
            hasPrivacyZones = zones.isNotEmpty(),
            shareMode = share,
            camera = if (all.isEmpty()) WorldMapUiState().camera else fit(all),
            bounds = if (framed.isEmpty()) emptyList() else listOf(
                GeoPoint(framed.minOf { it.latitude }, framed.minOf { it.longitude }),
                GeoPoint(framed.maxOf { it.latitude }, framed.maxOf { it.longitude }),
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
