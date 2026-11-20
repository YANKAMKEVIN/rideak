package com.rideak.shared.feature.places

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlacesFocusTest {
    private fun place(id: String) = Place(PlaceId(id), id, PlaceCategory.PARK, PlaceGeometry.Point(GeoPoint(49.0, 2.0)))

    private val state = PlacesUiState(
        loading = false,
        sections = listOf(
            PlaceSection(PlaceCategory.PARK, listOf(place("a"), place("b")), hidden = 3),
            PlaceSection(PlaceCategory.LAKE, emptyList(), hidden = 1),
            PlaceSection(PlaceCategory.RIVER, emptyList(), hidden = 2),
        ),
    )

    @Test
    fun homeShortcutScrollsToTheFirstSectionOfItsCategories() {
        assertEquals(1, state.sectionIndex(setOf(PlaceCategory.PARK)))
        // Park section: header + 2 places + "hidden" line.
        assertEquals(5, state.sectionIndex(setOf(PlaceCategory.RIVER, PlaceCategory.LAKE)))
        assertNull(state.sectionIndex(setOf(PlaceCategory.VIEWPOINT)))
    }

    @Test
    fun aRequestIsConsumedOnce() {
        val focus = PlacesFocus()
        focus.request(setOf(PlaceCategory.PARK))
        assertEquals(setOf(PlaceCategory.PARK), focus.categories.value)
        focus.consume()
        assertNull(focus.categories.value)
    }
}
