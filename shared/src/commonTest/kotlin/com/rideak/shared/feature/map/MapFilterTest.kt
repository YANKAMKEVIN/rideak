package com.rideak.shared.feature.map

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import com.rideak.exploration.PrivacyMask
import com.rideak.exploration.PrivacyZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapFilterTest {
    private val square = PlaceGeometry.Polygon(listOf(GeoPoint(49.0, 2.0), GeoPoint(49.0, 2.01), GeoPoint(49.01, 2.01), GeoPoint(49.01, 2.0)))
    private val park = Place(PlaceId("park"), "Parc François-Mitterrand", PlaceCategory.PARK, square)
    private val hiddenPark = Place(PlaceId("hidden"), "Parc caché", PlaceCategory.PARK, square)
    private val forest = Place(PlaceId("forest"), "Forêt de l'Hautil", PlaceCategory.FOREST, square)
    private val river = Place(
        PlaceId("oise"), "L'Oise", PlaceCategory.RIVER,
        PlaceGeometry.MultiLineString(
            listOf(
                PlaceGeometry.LineString(listOf(GeoPoint(49.02, 2.0), GeoPoint(49.03, 2.02))),
                PlaceGeometry.LineString(listOf(GeoPoint(49.03, 2.02), GeoPoint(49.04, 2.05))),
            ),
        ),
    )
    private val lake = Place(PlaceId("lake"), "Étangs de Cergy", PlaceCategory.LAKE, PlaceGeometry.MultiPolygon(listOf(square, square)))
    private val places = listOf(park, hiddenPark, forest, river, lake)
    private val found = setOf(park.id, forest.id, river.id, lake.id)
    private val noMask = PrivacyMask(emptyList())

    @Test
    fun allAndCitiesShowNoPlace() {
        assertEquals(PlaceOverlay(), placeOverlay(places, found, MapFilter.ALL, noMask))
        assertEquals(PlaceOverlay(), placeOverlay(places, found, MapFilter.CITIES, noMask))
    }

    @Test
    fun onlyDiscoveredPlacesOfTheCategoryAreShown() {
        val overlay = placeOverlay(places, found, MapFilter.PARKS, noMask)
        assertEquals(1, overlay.count)
        assertEquals(listOf(square), overlay.areas)
        assertTrue(overlay.lines.isEmpty() && overlay.points.isEmpty())
    }

    @Test
    fun waterGathersRiversAndLakesAndFlattensMultiGeometries() {
        val overlay = placeOverlay(places, found, MapFilter.WATER, noMask)
        assertEquals(2, overlay.count)
        assertEquals(2, overlay.lines.size)
        assertEquals(2, overlay.areas.size)
    }

    @Test
    fun nothingDiscoveredMeansAnEmptyOverlay() {
        assertEquals(0, placeOverlay(places, emptySet(), MapFilter.FORESTS, noMask).count)
    }

    @Test
    fun aPlaceTouchingAPrivacyZoneIsHiddenInShareMode() {
        val home = PrivacyMask(listOf(PrivacyZone("home", "Maison", GeoPoint(49.0, 2.0), 200.0)))
        assertEquals(0, placeOverlay(places, found, MapFilter.PARKS, home).count)
        assertEquals(1, placeOverlay(places, found, MapFilter.WATER, home).count)
    }

    @Test
    fun boundsCoverEveryPoint() {
        assertEquals(emptyList(), boundsOf(emptyList()))
        assertEquals(
            listOf(GeoPoint(49.0, 2.0), GeoPoint(49.04, 2.05)),
            boundsOf(placeOverlay(places, found, MapFilter.WATER, noMask).vertices),
        )
    }

    @Test
    fun polygonsAreWrittenWithClosedRings() {
        val json = GeoJson.polygons(listOf(PlaceGeometry.Polygon(listOf(GeoPoint(1.0, 2.0), GeoPoint(1.0, 3.0), GeoPoint(2.0, 3.0)))))
        assertTrue("[[[2.0,1.0],[3.0,1.0],[3.0,2.0],[2.0,1.0]]]" in json, json)
    }
}
