package com.rideak.shared.feature.places

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.rideak.exploration.PlaceCategory
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.icons.RideakIcons

/** How each place category is named and drawn, in one place. */
val PlaceCategory.label: String
    get() = when (this) {
        PlaceCategory.PARK -> "Parc"
        PlaceCategory.FOREST -> "Forêt"
        PlaceCategory.LAKE -> "Plan d'eau"
        PlaceCategory.RIVER -> "Rivière"
        PlaceCategory.VIEWPOINT -> "Point de vue"
        PlaceCategory.MONUMENT -> "Monument"
        PlaceCategory.LANDMARK -> "Curiosité"
    }

val PlaceCategory.plural: String
    get() = when (this) {
        PlaceCategory.PARK -> "Parcs"
        PlaceCategory.FOREST -> "Forêts"
        PlaceCategory.LAKE -> "Plans d'eau"
        PlaceCategory.RIVER -> "Rivières"
        PlaceCategory.VIEWPOINT -> "Points de vue"
        PlaceCategory.MONUMENT -> "Monuments"
        PlaceCategory.LANDMARK -> "Curiosités"
    }

val PlaceCategory.icon: ImageVector
    get() = when (this) {
        PlaceCategory.PARK -> RideakIcons.Park
        PlaceCategory.FOREST -> RideakIcons.Leaf
        PlaceCategory.LAKE, PlaceCategory.RIVER -> RideakIcons.River
        PlaceCategory.VIEWPOINT -> RideakIcons.Hills
        PlaceCategory.MONUMENT, PlaceCategory.LANDMARK -> RideakIcons.City
    }

/** Decorative tint of the category icon (never carries text). */
val PlaceCategory.tint: Color
    @Composable get() = when (this) {
        PlaceCategory.PARK, PlaceCategory.FOREST -> RideakTheme.colors.nature
        PlaceCategory.LAKE, PlaceCategory.RIVER -> RideakTheme.colors.exploration
        PlaceCategory.VIEWPOINT -> RideakTheme.colors.reward
        PlaceCategory.MONUMENT, PlaceCategory.LANDMARK -> RideakTheme.colors.action
    }
