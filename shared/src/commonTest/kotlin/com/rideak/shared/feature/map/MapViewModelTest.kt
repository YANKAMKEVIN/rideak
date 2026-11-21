package com.rideak.shared.feature.map

import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import com.rideak.exploration.RegionId
import com.rideak.exploration.RegionPackCodec
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import com.rideak.shared.FakePrivacyZoneRepository
import com.rideak.shared.FakeRideRepository
import com.rideak.shared.di.DeveloperSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The map tab stays alive: it must follow finished rides, not load them once. */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val noTracks = GeoJson.lines(emptyList())

    /** Same order as the recorder: the ride is created, its points written, then it is marked finished. */
    private suspend fun FakeRideRepository.finishRide(id: String) {
        val summary = RideSummary(RideId(id), 0L, null, RideStatus.RECORDING, 60_000L, 300.0, 60_000L, 5.0, 0.0)
        createRide(summary)
        // 60 s east at 5 m/s, one fix per second, like a real ride.
        val points = List(60) {
            TrackPoint(GeoPoint(49.0364, 2.0761 + it * 0.0000685), it * 1_000L, horizontalAccuracyMeters = 5f, speedMetersPerSecond = 5f)
        }
        appendPoints(RideId(id), 0, points)
        saveSummary(summary.copy(status = RideStatus.FINISHED, endedAtEpochMillis = 60_000L))
    }

    /** Waits in real time: the overlays are computed on Dispatchers.Default. */
    private suspend fun MapViewModel.awaitState(predicate: (WorldMapUiState) -> Boolean) =
        withContext(Dispatchers.Default) { withTimeout(10_000) { state.first { !it.loading && predicate(it) } } }

    @Test
    fun aRideFinishedAfterOpeningTheMapAppearsAndDeletingDataClearsIt() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val rides = FakeRideRepository()
        val vm = MapViewModel(rides, DeveloperSettings(), { emptyList() }, FakePrivacyZoneRepository())
        backgroundScope.launch { vm.state.collect {} }

        vm.awaitState { it.tracks == noTracks }

        rides.finishRide("r1")
        vm.awaitState { it.tracks != noTracks && it.exploredCells > 0 }

        rides.deleteFinishedRides()
        vm.awaitState { it.tracks == noTracks && it.exploredCells == 0 }
    }

    @Test
    fun cardsFilterTheDiscoveredPlacesAndFrameThem() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val rides = FakeRideRepository()
        // Both places sit on the ride (49.0364, 2.0761 → 2.0802); the forest far away is never reached.
        val park = Place(PlaceId("park"), "Parc", PlaceCategory.PARK, PlaceGeometry.Point(GeoPoint(49.0364, 2.0770)))
        val river = Place(PlaceId("river"), "L'Oise", PlaceCategory.RIVER, PlaceGeometry.Point(GeoPoint(49.0364, 2.0790)))
        val forest = Place(PlaceId("forest"), "Forêt", PlaceCategory.FOREST, PlaceGeometry.Point(GeoPoint(49.2, 2.3)))
        val region = ExplorationRegion(
            RegionId("r"), "Cergy",
            listOf(PlaceGeometry.Polygon(listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.2), GeoPoint(49.1, 2.2)))),
            "tile17", emptySet(),
        )
        val vm = MapViewModel(rides, DeveloperSettings(), { listOf(RegionPackCodec.Pack(region, "OSM", listOf(park, river, forest))) }, FakePrivacyZoneRepository())
        backgroundScope.launch { vm.state.collect {} }
        rides.finishRide("r1")

        val all = vm.awaitState { it.shownPlaces.isNotEmpty() }
        assertEquals(setOf("Parc", "L'Oise"), all.shownPlaces.map { it.name }.toSet())

        vm.selectFilter(MapFilter.PARKS)
        val parks = vm.awaitState { it.filter == MapFilter.PARKS }
        assertEquals(listOf("Parc"), parks.shownPlaces.map { it.name })
        assertEquals(listOf(GeoPoint(49.0364, 2.0770), GeoPoint(49.0364, 2.0770)), parks.bounds)

        vm.selectFilter(MapFilter.PARKS)
        vm.awaitState { it.filter == MapFilter.ALL && it.shownPlaces.size == 2 }
    }

    @Test
    fun aCardWithNothingDiscoveredSaysSo() {
        val places = mapOf(PlaceCategory.PARK to Counter(1, 4), PlaceCategory.LAKE to Counter(0, 2), PlaceCategory.RIVER to Counter(0, 1))
        assertNull(MapFilter.ALL.emptyHint(places))
        assertNull(MapFilter.PARKS.emptyHint(places))
        assertEquals("Aucune rivière ni plan d'eau découvert pour l'instant : pars à leur recherche !", MapFilter.RIVERS.emptyHint(places))
        assertEquals("Aucune forêt découverte pour l'instant : pars à leur recherche !", MapFilter.FORESTS.emptyHint(places), "no forest in the region")
    }
}
