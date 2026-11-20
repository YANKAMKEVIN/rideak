package com.rideak.shared.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
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
import com.rideak.shared.feature.map.MapFocus
import com.rideak.shared.feature.map.WorldMapScreen
import com.rideak.shared.feature.legal.LegalScreen
import com.rideak.shared.feature.onboarding.OnboardingScreen
import com.rideak.shared.feature.places.PlacesFocus
import com.rideak.shared.feature.places.PlacesScreen
import com.rideak.shared.feature.profile.ProfileScreen
import com.rideak.shared.feature.ride.RideScreen
import com.rideak.shared.feature.rides.RidesScreen
import com.rideak.shared.feature.summary.RideSummaryScreen
import com.rideak.shared.settings.AppSettings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject

/** Root of the shared UI, hosted by the Android Activity and the iOS UIViewController. */
@Composable
fun RideakApp() {
    RideakTheme {
        val settings = koinInject<AppSettings>()
        val onboardingDone by settings.onboardingDone.collectAsStateWithLifecycle()
        // Set by the onboarding's last button: the ride screen opens (and asks for the location) right after.
        var startRide by rememberSaveable { mutableStateOf(false) }
        if (!onboardingDone) {
            OnboardingScreen(
                onStartRide = {
                    startRide = true
                    settings.setOnboardingDone()
                },
                onSkip = settings::setOnboardingDone,
            )
        } else {
            MainNavigation(startRide, onRideStarted = { startRide = false })
        }
    }
}

@Composable
private fun MainNavigation(startRide: Boolean, onRideStarted: () -> Unit) {
    val navController = rememberNavController()
    val placesFocus = koinInject<PlacesFocus>()
    val mapFocus = koinInject<MapFocus>()
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
                    onSelect = { index -> navController.navigateToTab(topLevelDestinations[index].route) },
                )
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            composable<HomeRoute> {
                HomeScreen(
                    onRide = { navController.navigate(RideRoute) { launchSingleTop = true } },
                    onOpenMap = { navController.navigateToTab(MapRoute) },
                    onOpenCategory = { category ->
                        placesFocus.request(category.categories)
                        navController.navigateToTab(PlacesRoute)
                    },
                )
            }
            composable<MapRoute> { WorldMapScreen() }
            composable<RidesRoute> {
                RidesScreen(
                    onOpenRide = { navController.navigate(RideSummaryRoute(it)) },
                    onStartRide = { navController.navigate(RideRoute) { launchSingleTop = true } },
                )
            }
            composable<PlacesRoute> {
                PlacesScreen(
                    onOpenPlace = { place ->
                        mapFocus.request(place)
                        navController.navigateToTab(MapRoute)
                    },
                )
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onPrivacyPolicy = { navController.navigate(PrivacyPolicyRoute) },
                    onCredits = { navController.navigate(CreditsRoute) },
                )
            }
            composable<PrivacyPolicyRoute> { LegalScreen("files/legal/privacy_policy.md", onBack = { navController.popBackStack() }) }
            composable<CreditsRoute> { LegalScreen("files/legal/credits.md", onBack = { navController.popBackStack() }) }
            // The ride screen rises from the bottom and goes back down when minimised, where its chevron points.
            composable<RideRoute>(
                enterTransition = { slideInVertically(tween(350)) { it } },
                popExitTransition = { slideOutVertically(tween(300)) { it } },
            ) {
                RideScreen(
                    onFinished = { rideId ->
                        navController.navigate(RideSummaryRoute(rideId.value, fresh = true)) {
                            popUpTo<RideRoute> { inclusive = true }
                        }
                    },
                    onMinimize = { navController.popBackStack() },
                )
            }
            composable<RideSummaryRoute> { entry ->
                val route = entry.toRoute<RideSummaryRoute>()
                RideSummaryScreen(route.rideId, fresh = route.fresh, onBack = { navController.popBackStack() })
            }
        }
        LaunchedEffect(startRide) {
            if (startRide) {
                onRideStarted()
                navController.navigate(RideRoute) { launchSingleTop = true }
            }
        }
    }
}

/** Switches bottom-bar tab, keeping each tab's state (same as tapping the bar). */
private fun NavController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
