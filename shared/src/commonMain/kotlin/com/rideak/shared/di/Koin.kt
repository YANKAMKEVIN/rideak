package com.rideak.shared.di

import com.rideak.data.ApplicationScope
import com.rideak.data.dataModule
import com.rideak.data.platformDataModule
import com.rideak.data.recorder.RideRecorder
import com.rideak.shared.feature.home.HomeViewModel
import com.rideak.shared.feature.profile.ProfileViewModel
import com.rideak.shared.feature.ride.RideViewModel
import com.rideak.shared.feature.rides.RidesViewModel
import com.rideak.shared.feature.summary.RideSummaryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Facts about the running build, provided by each host app. */
data class AppInfo(val isDebug: Boolean)

private val uiModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::RideViewModel)
    viewModelOf(::RidesViewModel)
    viewModelOf(::ProfileViewModel)
    viewModel { (rideId: String) -> RideSummaryViewModel(rideId, get()) }
}

/** Called once by each host app at startup. */
fun initKoin(platform: KoinAppDeclaration = {}) {
    val koin = startKoin {
        platform()
        modules(dataModule, platformDataModule, uiModule)
    }.koin
    // A ride left unfinished by a killed process comes back paused.
    koin.get<CoroutineScope>(ApplicationScope).launch { koin.get<RideRecorder>().recoverUnfinished() }
}
