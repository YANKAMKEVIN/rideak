package com.rideak.exploration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceDiscovererTest {
    private val discoverer = PlaceDiscoverer()

    // ~73 m per 0.001° of longitude and ~111 m per 0.001° of latitude at 49° N.
    private fun east(meters: Double) = meters / 73.0 * 0.001
    private fun north(meters: Double) = meters / 111.2 * 0.001

    private val origin = GeoPoint(49.0364, 2.0700)
    private fun at(eastMeters: Double, northMeters: Double = 0.0) =
        GeoPoint(origin.latitude + north(northMeters), origin.longitude + east(eastMeters))

    /** Riding due east from the origin for [meters], one fix every 50 m. */
    private fun rideEast(meters: Double) = listOf((0..(meters / 50).toInt()).map { at(it * 50.0) })

    private fun place(id: String, category: PlaceCategory, geometry: PlaceGeometry) = Place(PlaceId(id), id, category, geometry)

    private fun square(centerEast: Double, centerNorth: Double, half: Double) = PlaceGeometry.Polygon(
        listOf(
            at(centerEast - half, centerNorth - half), at(centerEast + half, centerNorth - half),
            at(centerEast + half, centerNorth + half), at(centerEast - half, centerNorth + half),
        ),
    )

    @Test
    fun monumentIsDiscoveredWhenPassingClose() {
        val near = place("near", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(500.0, 30.0)))
        val far = place("far", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(500.0, 120.0)))
        assertEquals(setOf(PlaceId("near")), discoverer.discovered(listOf(near, far), rideEast(1_000.0)))
    }

    @Test
    fun monumentBetweenTwoDistantFixesIsStillDiscovered() {
        // Fixes 400 m apart; the monument sits next to the path, half-way.
        val monument = place("m", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(200.0, 10.0)))
        assertEquals(setOf(PlaceId("m")), discoverer.discovered(listOf(monument), listOf(listOf(at(0.0), at(400.0)))))
    }

    @Test
    fun riverIsDiscoveredWhenCrossed() {
        val river = place(
            "oise", PlaceCategory.RIVER,
            PlaceGeometry.MultiLineString(
                listOf(
                    PlaceGeometry.LineString(listOf(at(300.0, -500.0), at(300.0, -100.0))),
                    PlaceGeometry.LineString(listOf(at(300.0, -100.0), at(320.0, 500.0))),
                ),
            ),
        )
        assertEquals(setOf(PlaceId("oise")), discoverer.discovered(listOf(river), rideEast(1_000.0)))
        assertTrue(discoverer.discovered(listOf(river), rideEast(200.0)).isEmpty(), "stopped 100 m before it")
    }

    @Test
    fun parkMustBeEnteredButLakeOnlyApproached() {
        // Both areas lie 20 m north of the path, never crossed by it.
        val park = place("park", PlaceCategory.PARK, square(centerEast = 500.0, centerNorth = 120.0, half = 100.0))
        val lake = place("lake", PlaceCategory.LAKE, square(centerEast = 500.0, centerNorth = 120.0, half = 100.0))
        assertEquals(setOf(PlaceId("lake")), discoverer.discovered(listOf(park, lake), rideEast(1_000.0)))
        val throughPark = place("crossed", PlaceCategory.PARK, square(centerEast = 500.0, centerNorth = 0.0, half = 100.0))
        assertEquals(setOf(PlaceId("crossed")), discoverer.discovered(listOf(throughPark), rideEast(1_000.0)))
    }

    @Test
    fun onlyFirstDiscoveriesAreNew() {
        val a = place("a", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(100.0)))
        val b = place("b", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(900.0)))
        val before = rideEast(300.0)
        val ride = rideEast(1_000.0)
        assertEquals(setOf(PlaceId("b")), discoverer.newlyDiscovered(listOf(a, b), ride, before))
    }

    @Test
    fun engineAndDiscovererAgreeOnGaps() {
        // A 2 km jump in 30 s (train) is not ridden: the monument in the middle stays undiscovered.
        val engine = ExplorationEngine.standard()
        val monument = place("m", PlaceCategory.MONUMENT, PlaceGeometry.Point(at(1_000.0)))
        val jump = listOf(TrackPoint(at(0.0), 0), TrackPoint(at(2_000.0), 30_000))
        assertTrue(discoverer.discovered(listOf(monument), engine.pathsFor(listOf(jump))).isEmpty())
    }
}
