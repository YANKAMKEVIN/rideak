package com.rideak.shared.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.RideakBottomBar
import com.rideak.shared.feature.home.HomeScreen
import com.rideak.shared.feature.map.WorldMapScreen
import com.rideak.shared.feature.placeholder.PlaceholderScreen
import com.rideak.shared.feature.profile.ProfileScreen
import com.rideak.shared.feature.ride.RideScreen
import com.rideak.shared.feature.rides.RidesScreen
import com.rideak.shared.feature.summary.RideSummaryScreen

/** Root of the shared UI, hosted by the Android Activity and the iOS UIViewController. */
@Composable
fun RideakApp() {
    RideakTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val selectedIndex = topLevelDestinations.indexOfFirst { top ->
            destination?.hierarchy?.any { it.hasRoute(top.route::class) } == true
        }

        Scaffold(
            containerColor = RideakTheme.colors.night,
            // Screens handle the status bar themselves: Home draws its hero under it.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // Hidden on full-screen destinations (ride, summary).
                if (selectedIndex >= 0) {
                    RideakBottomBar(
                        items = topLevelDestinations.map { it.item },
                        selectedIndex = selectedIndex,
                        onSelect = { index ->
                            navController.navigate(topLevelDestinations[index].route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
                composable<HomeRoute> { HomeScreen(onRide = { navController.navigate(RideRoute) { launchSingleTop = true } }) }
                composable<MapRoute> { WorldMapScreen() }
                composable<RidesRoute> { RidesScreen(onOpenRide = { navController.navigate(RideSummaryRoute(it)) }) }
                composable<PlacesRoute> { PlaceholderScreen("Lieux", "Tes lieux découverts et tes ❤️ arrivent bientôt.") }
                composable<ProfileRoute> { ProfileScreen() }
                composable<RideRoute> {
                    RideScreen(
                        onFinished = { rideId ->
                            navController.navigate(RideSummaryRoute(rideId.value)) {
                                popUpTo<RideRoute> { inclusive = true }
                            }
                        },
                        onMinimize = { navController.popBackStack() },
                    )
                }
                composable<RideSummaryRoute> { entry ->
                    RideSummaryScreen(entry.toRoute<RideSummaryRoute>().rideId, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
