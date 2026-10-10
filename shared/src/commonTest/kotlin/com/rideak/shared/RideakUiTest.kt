package com.rideak.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.navigation.RideakApp
import com.rideak.data.RideRepository
import com.rideak.data.location.LocationSource
import com.rideak.data.location.LocationSourceSelector
import com.rideak.data.recorder.RideRecorder
import com.rideak.shared.di.AppInfo
import com.rideak.shared.feature.home.HomeViewModel
import com.rideak.shared.feature.profile.ProfileViewModel
import com.rideak.shared.feature.rides.RidesViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RideakUiTest {
    @BeforeTest
    fun startDi() {
        startKoin {
            modules(
                module {
                    val noGps = LocationSource { emptyFlow() }
                    single<RideRepository> { FakeRideRepository() }
                    single { LocationSourceSelector(noGps, noGps) }
                    single { RideRecorder(get(), { noGps }, MainScope(), { 0L }, { "id" }) }
                    single { AppInfo(isDebug = true) }
                    viewModelOf(::HomeViewModel)
                    viewModelOf(::RidesViewModel)
                    viewModelOf(::ProfileViewModel)
                },
            )
        }
    }

    @AfterTest
    fun stopDi() = stopKoin()

    @Test
    fun adventureButtonIsClickableWithLargeTouchTarget() = runComposeUiTest {
        var clicked = false
        setContent { RideakTheme { AdventureButton("PARTIR", onClick = { clicked = true }) } }

        onNodeWithText("PARTIR").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(clicked)
    }

    @Test
    fun bottomBarNavigatesBetweenTopLevelDestinations() = runComposeUiTest {
        setContent { RideakApp() }

        onNodeWithText("Explorer autour de moi").assertIsDisplayed()
        onNodeWithContentDescription("Mon territoire, Ta première sortie dévoilera ta carte").assertIsDisplayed()
        onNodeWithText("Sorties").performClick()
        onNodeWithText("Aucune sortie pour l'instant. Pars à l'aventure depuis l'accueil !").assertIsDisplayed()
        onNodeWithText("Profil").performClick()
        onNodeWithText("Sortie simulée").assertIsDisplayed()
    }
}
