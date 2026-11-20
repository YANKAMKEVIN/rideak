package com.rideak.shared

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.rideak.shared.designsystem.RideakTheme
import com.rideak.shared.designsystem.components.AdventureButton
import com.rideak.shared.designsystem.components.RevealBanner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rideak.shared.navigation.RideakApp
import com.rideak.data.RideRepository
import com.rideak.data.location.LocationSource
import com.rideak.data.location.LocationSourceSelector
import com.rideak.data.recorder.RideRecorder
import com.rideak.shared.di.AppInfo
import com.rideak.shared.di.DeveloperSettings
import com.rideak.shared.settings.AppSettings
import com.rideak.shared.settings.InMemorySettingsStorage
import com.rideak.shared.feature.home.HomeViewModel
import com.rideak.shared.feature.profile.ProfileViewModel
import com.rideak.shared.feature.profile.PrivacySection
import com.rideak.shared.feature.profile.PrivacyUiState
import com.rideak.data.PrivacyZoneRepository
import com.rideak.exploration.PrivacySuggestion
import com.rideak.exploration.PrivacyZone
import kotlin.test.assertEquals
import com.rideak.shared.feature.rides.RidesScreen
import com.rideak.shared.feature.rides.RidesViewModel
import com.rideak.shared.feature.places.PlacesViewModel
import com.rideak.shared.feature.legal.LegalDocument
import com.rideak.shared.feature.legal.parseMarkdown
import com.rideak.shared.feature.map.OfflineMapSection
import com.rideak.shared.feature.map.OfflineMapState
import com.rideak.shared.feature.map.OfflineMapStore
import com.rideak.shared.feature.map.OfflineMapUiState
import com.rideak.shared.feature.map.OfflineMapViewModel
import com.rideak.shared.feature.places.NewPlaces
import com.rideak.shared.feature.onboarding.OnboardingScreen
import com.rideak.shared.feature.places.PlaceSection
import com.rideak.shared.feature.places.PlacesContent
import com.rideak.shared.feature.places.PlacesUiState
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.Badge
import com.rideak.exploration.PlayerStats
import com.rideak.exploration.Progression
import com.rideak.shared.feature.progress.BadgeGrid
import com.rideak.shared.feature.progress.LevelCard
import com.rideak.shared.feature.progress.RideRewards
import androidx.compose.foundation.layout.Column
import com.rideak.exploration.Place
import com.rideak.exploration.PlaceCategory
import com.rideak.exploration.PlaceGeometry
import com.rideak.exploration.PlaceId
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.TerritoryProgress
import com.rideak.shared.designsystem.components.TerritoryCard
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
                    single<PrivacyZoneRepository> { FakePrivacyZoneRepository() }
                    single { LocationSourceSelector(noGps, noGps) }
                    single { RideRecorder(get(), { noGps }, MainScope(), { 0L }, { "id" }) }
                    single { AppInfo(isDebug = true) }
                    single { DeveloperSettings() }
                    single { AppSettings(InMemorySettingsStorage(mapOf(AppSettings.ONBOARDING_DONE to true))) }
                    single<RegionSource> { RegionSource { emptyList() } }
                    viewModelOf(::PlacesViewModel)
                    single<OfflineMapStore> { FakeOfflineMapStore() }
                    viewModelOf(::OfflineMapViewModel)
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
        onNodeWithContentDescription("Mon territoire, Ta première sortie dévoilera ta carte, prochain palier 10 zones").assertIsDisplayed()
        onNodeWithText("Sorties").performClick()
        onNodeWithText("Aucune sortie pour l'instant").assertIsDisplayed()
        onNodeWithText("Profil").performClick()
        onNodeWithContentDescription("Niveau 1, Novice, 0 XP, prochain niveau à 100 XP").assertIsDisplayed()
        onNodeWithText("Sortie simulée").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun onboardingWalksThroughThreePanesThenStartsTheFirstRide() = runComposeUiTest {
        var started = false
        var skipped = false
        setContent { RideakTheme { OnboardingScreen(onStartRide = { started = true }, onSkip = { skipped = true }) } }

        onNodeWithText("EXPLORE TON MONDE À VÉLO").assertIsDisplayed()
        onNodeWithText("SUIVANT").performClick()
        onNodeWithText("DÉCOUVRE LES LIEUX CACHÉS").assertIsDisplayed()
        onNodeWithText("SUIVANT").performClick()
        onNodeWithText("TES TRACES RESTENT SUR TON TÉLÉPHONE").assertIsDisplayed()
        onNodeWithText("PARTIR\nÀ L'AVENTURE").assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(started)
        assertTrue(!skipped)
    }

    @Test
    fun emptyRidesListOffersToStartTheFirstRide() = runComposeUiTest {
        var started = false
        setContent {
            RideakTheme { RidesScreen(onOpenRide = {}, onStartRide = { started = true }, viewModel = RidesViewModel(FakeRideRepository())) }
        }

        onNodeWithText("Aucune sortie pour l'instant").assertIsDisplayed()
        onNodeWithText("PARTIR\nÀ L'AVENTURE").assertHasClickAction().assertHeightIsAtLeast(48.dp).performClick()
        assertTrue(started)
    }

    @Test
    fun revealBannerCelebratesNewCellsAndStaysQuietOnKnownGround() = runComposeUiTest {
        var revealed by mutableStateOf(12)
        setContent { RideakTheme { RevealBanner(revealed = revealed, traversed = 30) } }
        onNodeWithContentDescription("+12 zones dévoilées ! sur 30 traversées · ta carte s'agrandit").assertIsDisplayed()
        revealed = 1
        onNodeWithContentDescription("+1 zone dévoilée ! sur 30 traversées · ta carte s'agrandit").assertIsDisplayed()
        revealed = 0
        onNodeWithContentDescription("Terrain connu", substring = true).assertIsDisplayed()
    }

    @Test
    fun territoryCardShowsTheRealShareOfARegion() = runComposeUiTest {
        setContent { RideakTheme { TerritoryCard(exploredCells = 60, territory = TerritoryProgress("Cergy", 47, 391)) } }
        onNodeWithContentDescription("Mon territoire, Cergy explorée à 12 %, 47 / 391 zones praticables").assertIsDisplayed()
    }

    private val oise = Place(PlaceId("way/1"), "L'Oise", PlaceCategory.RIVER, PlaceGeometry.Point(GeoPoint(49.0, 2.0)))
    private val parc = Place(PlaceId("way/2"), "Parc François-Mitterrand", PlaceCategory.PARK, PlaceGeometry.Point(GeoPoint(49.0, 2.0)))

    @Test
    fun newPlacesAreAnnouncedAtTheEndOfARide() = runComposeUiTest {
        setContent { RideakTheme { NewPlaces(listOf(oise, parc)) } }
        onNodeWithText("2 NOUVEAUX ENDROITS !").assertIsDisplayed()
        onNodeWithContentDescription("Rivière : L'Oise").assertIsDisplayed()
        onNodeWithContentDescription("Parc : Parc François-Mitterrand").assertIsDisplayed()
    }

    @Test
    fun placesScreenShowsDiscoveredNamesAndHidesTheRest() = runComposeUiTest {
        val state = PlacesUiState(
            loading = false,
            sections = listOf(PlaceSection(PlaceCategory.PARK, listOf(parc), hidden = 3), PlaceSection(PlaceCategory.RIVER, emptyList(), hidden = 1)),
        )
        setContent { RideakTheme { PlacesContent(state) } }
        onNodeWithText("1 / 5 lieux découverts").assertIsDisplayed()
        onNodeWithContentDescription("Parc : Parc François-Mitterrand").assertIsDisplayed()
        onNodeWithText("3 lieux encore cachés").assertIsDisplayed()
        onNodeWithText("1 lieu encore caché").assertIsDisplayed()
    }

    @Test
    fun privacySectionOffersToProtectFrequentStarts() = runComposeUiTest {
        val home = GeoPoint(49.0364, 2.0761)
        var protected: String? = null
        var state by mutableStateOf(PrivacyUiState(suggestions = listOf(PrivacySuggestion(home, rides = 4))))
        setContent { RideakTheme { PrivacySection(state, onProtect = { _, label -> protected = label }, onRemove = {}) } }
        onNodeWithText("4 sorties partent ou arrivent au même endroit. Le protéger ?").assertIsDisplayed()
        onNodeWithText("Domicile").assertHasClickAction().performClick()
        assertEquals("Domicile", protected)
        state = PrivacyUiState(zones = listOf(PrivacyZone("z", "Domicile", home, 437.0)))
        onNodeWithText("Domicile · 440 m").assertIsDisplayed()
        onNodeWithText("Retirer").assertHasClickAction()
    }

    @Test
    fun profileShowsLevelAndBadges() = runComposeUiTest {
        val stats = PlayerStats(rides = 2, distanceMeters = 12_000.0, exploredCells = 8)
        setContent {
            RideakTheme {
                Column {
                    LevelCard(Progression.level(Progression.xp(stats)))
                    BadgeGrid(stats)
                }
            }
        }
        // 8 × 10 + 2 × 20 + 12 × 5 = 180 XP: level 2.
        onNodeWithContentDescription("Niveau 2, Éclaireur, 180 XP, prochain niveau à 300 XP").assertIsDisplayed()
        onNodeWithText("2 / 6").assertIsDisplayed()
        onNodeWithContentDescription("Badge 10 km, débloqué").assertIsDisplayed()
        onNodeWithContentDescription("Badge Explorateur, verrouillé, Explore 100 zones, 8 sur 100").assertIsDisplayed()
    }

    @Test
    fun rideRewardsAnnounceXpAndBadges() = runComposeUiTest {
        setContent { RideakTheme { RideRewards(xp = 140, badges = listOf(Badge.FIRST_RIDE)) } }
        onNodeWithText("+140 XP").assertIsDisplayed()
        onNodeWithContentDescription("Badge débloqué : 1re sortie").assertIsDisplayed()
    }

    @Test
    fun offlineMapSectionFollowsTheDownload() = runComposeUiTest {
        var state by mutableStateOf(OfflineMapUiState("Cergy", OfflineMapState.NotDownloaded))
        var downloads = 0
        setContent { RideakTheme { OfflineMapSection(state, onDownload = { downloads++ }, onDelete = {}) } }
        onNodeWithText("Télécharger Cergy").performClick()
        assertEquals(1, downloads)
        state = state.copy(map = OfflineMapState.Downloading(fraction = 0.4f, bytes = 1_500_000))
        onNodeWithText("Téléchargement de Cergy… 1,5 Mo").assertIsDisplayed()
        state = state.copy(map = OfflineMapState.Ready(4_200_000))
        onNodeWithText("Cergy est disponible hors ligne · 4,2 Mo").assertIsDisplayed()
        onNodeWithText("Supprimer").assertHasClickAction()
    }

    @Test
    fun legalDocumentRendersHeadingsAndBackArrow() = runComposeUiTest {
        var back = false
        val blocks = parseMarkdown("# Crédits et licences\n\n## Données cartographiques\n\nDonnées © [OpenStreetMap](https://www.openstreetmap.org/copyright).")
        setContent { RideakTheme { LegalDocument(blocks, onBack = { back = true }) } }
        onNodeWithText("Crédits et licences").assertIsDisplayed()
        onNodeWithText("Données cartographiques").assertIsDisplayed()
        onNodeWithText("Données © OpenStreetMap.").assertIsDisplayed()
        onNodeWithContentDescription("Retour").performClick()
        assertTrue(back)
    }
}
