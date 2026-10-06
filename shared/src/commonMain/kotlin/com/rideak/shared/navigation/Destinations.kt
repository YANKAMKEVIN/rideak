package com.rideak.shared.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import com.rideak.shared.designsystem.components.BottomBarItem
import kotlinx.serialization.Serializable

// Top-level destinations. ActiveRide, RideSummary and Discovery arrive with their features.
@Serializable data object HomeRoute
@Serializable data object MapRoute
@Serializable data object RidesRoute
@Serializable data object PlacesRoute
@Serializable data object ProfileRoute

internal data class TopLevelDestination(val route: Any, val item: BottomBarItem)

// Temporary Material icons; the illustrated RIDEAK icon set replaces them later.
@Suppress("DEPRECATION")
internal val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, BottomBarItem("Accueil", Icons.Filled.Home)),
    TopLevelDestination(MapRoute, BottomBarItem("Carte", Icons.Filled.LocationOn)),
    TopLevelDestination(RidesRoute, BottomBarItem("Sorties", Icons.Filled.List)),
    TopLevelDestination(PlacesRoute, BottomBarItem("Lieux", Icons.Filled.Star)),
    TopLevelDestination(ProfileRoute, BottomBarItem("Profil", Icons.Filled.Person)),
)
