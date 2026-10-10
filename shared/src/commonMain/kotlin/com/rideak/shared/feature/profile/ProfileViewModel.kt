package com.rideak.shared.feature.profile

import androidx.lifecycle.ViewModel
import com.rideak.data.location.LocationSourceSelector
import com.rideak.shared.di.AppInfo
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(private val selector: LocationSourceSelector, appInfo: AppInfo) : ViewModel() {
    val showDeveloperTools = appInfo.isDebug
    val simulation: StateFlow<Boolean> = selector.useSimulation

    fun setSimulation(enabled: Boolean) = selector.setUseSimulation(enabled)
}
