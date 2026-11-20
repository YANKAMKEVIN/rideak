package com.rideak.shared.feature.places

import com.rideak.exploration.PlaceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A request, from another tab, to open "Lieux" on a category (the home category shortcuts).
 * The Places screen scrolls to the first section of [categories] and clears the request.
 */
class PlacesFocus {
    private val _categories = MutableStateFlow<Set<PlaceCategory>?>(null)
    val categories: StateFlow<Set<PlaceCategory>?> = _categories.asStateFlow()

    fun request(categories: Set<PlaceCategory>) {
        _categories.value = categories
    }

    fun consume() {
        _categories.value = null
    }
}
