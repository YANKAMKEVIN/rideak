package com.rideak.shared.feature.map

import com.rideak.exploration.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A request, from another tab, to open "Ma carte" centred on a place (a discovered place in "Lieux"). */
class MapFocus {
    private val _place = MutableStateFlow<Place?>(null)
    val place: StateFlow<Place?> = _place.asStateFlow()

    fun request(place: Place) {
        _place.value = place
    }

    fun consume() {
        _place.value = null
    }
}
