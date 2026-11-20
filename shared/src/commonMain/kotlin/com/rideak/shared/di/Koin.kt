package com.rideak.shared.di

import com.rideak.data.ApplicationScope
import com.rideak.data.dataModule
import com.rideak.data.platformDataModule
import com.rideak.data.recorder.RideRecorder
import com.rideak.shared.feature.home.HomeViewModel
import com.rideak.shared.feature.map.MapLibreOfflineMapStore
import com.rideak.shared.feature.map.MapViewModel
import com.rideak.shared.feature.map.OfflineMapStore
import com.rideak.shared.feature.map.OfflineMapViewModel
import com.rideak.shared.feature.places.PlacesFocus
import com.rideak.shared.feature.places.PlacesViewModel
import com.rideak.shared.feature.profile.ProfileViewModel
import com.rideak.shared.feature.ride.RideViewModel
import com.rideak.shared.feature.rides.RidesViewModel
import com.rideak.shared.feature.summary.RideSummaryViewModel
import com.rideak.shared.feature.territory.BundledRegions
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.settings.AppSettings
import com.rideak.shared.settings.platformSettingsModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Facts about the running build, provided by each host app. */
data class AppInfo(val isDebug: Boolean)

/** Debug-only switches, kept in memory (reset at each launch). */
class DeveloperSettings {
    /** Adds a synthetic ~1 000-cell exploration to the map, to measure overlay rendering. */
    val mapStressTest = kotlinx.coroutines.flow.MutableStateFlow(false)
}

private val uiModule = module {
    single { DeveloperSettings() }
    single { AppSettings(get()) }
    single { PlacesFocus() }
    single<RegionSource> { BundledRegions() }
    single<OfflineMapStore> { MapLibreOfflineMapStore() }
    viewModelOf(::OfflineMapViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::MapViewModel)
    viewModelOf(::RideViewModel)
    viewModelOf(::RidesViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::PlacesViewModel)
    viewModel { (rideId: String) -> RideSummaryViewModel(rideId, get(), get()) }
}

/** Called once by each host app at startup. */
fun initKoin(platform: KoinAppDeclaration = {}) {
    val koin = startKoin {
        platform()
        modules(dataModule, platformDataModule, platformSettingsModule, uiModule)
    }.koin
    // A ride left unfinished by a killed process comes back paused.
    koin.get<CoroutineScope>(ApplicationScope).launch { koin.get<RideRecorder>().recoverUnfinished() }
}
