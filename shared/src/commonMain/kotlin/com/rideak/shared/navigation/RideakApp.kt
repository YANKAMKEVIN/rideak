package com.rideak.shared.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.RideakBottomBar
import com.rideak.shared.feature.home.HomeScreen
import com.rideak.shared.feature.placeholder.PlaceholderScreen

/** Root of the shared UI, hosted by the Android Activity and the iOS UIViewController. */
@Composable
fun RideakApp() {
    RideakTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val selectedIndex = topLevelDestinations.indexOfFirst { destination ->
            backStackEntry?.destination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
        }.coerceAtLeast(0)
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            containerColor = RideakTheme.colors.night,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
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
            },
        ) { padding ->
            NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
                composable<HomeRoute> { HomeScreen(snackbarHostState) }
                composable<MapRoute> { PlaceholderScreen("Carte", "Ta world map personnelle se dévoilera ici.") }
                composable<RidesRoute> { PlaceholderScreen("Sorties", "L'historique de tes sorties arrive bientôt.") }
                composable<PlacesRoute> { PlaceholderScreen("Lieux", "Tes lieux découverts et tes ❤️ arrivent bientôt.") }
                composable<ProfileRoute> { PlaceholderScreen("Profil", "Ta progression arrive bientôt.") }
            }
        }
    }
}
