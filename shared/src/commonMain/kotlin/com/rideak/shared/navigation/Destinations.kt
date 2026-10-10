package com.rideak.shared.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import com.rideak.shared.designsystem.components.BottomBarItem
import com.rideak.shared.designsystem.icons.RideakIcons
import kotlinx.serialization.Serializable

// Top-level destinations (bottom bar).
@Serializable data object HomeRoute
@Serializable data object MapRoute
@Serializable data object RidesRoute
@Serializable data object PlacesRoute
@Serializable data object ProfileRoute

// Full-screen destinations (no bottom bar). Discovery arrives with its feature.
@Serializable data object RideRoute
@Serializable data class RideSummaryRoute(val rideId: String)

internal data class TopLevelDestination(val route: Any, val item: BottomBarItem)

// Outline icons as in the mockup; the illustrated RIDEAK icon set replaces them later.
internal val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, BottomBarItem("Accueil", Icons.Outlined.Home)),
    TopLevelDestination(MapRoute, BottomBarItem("Carte", RideakIcons.Map)),
    TopLevelDestination(RidesRoute, BottomBarItem("Sorties", RideakIcons.Bike)),
    TopLevelDestination(PlacesRoute, BottomBarItem("Lieux", Icons.Outlined.LocationOn)),
    TopLevelDestination(ProfileRoute, BottomBarItem("Profil", Icons.Outlined.Person)),
)
