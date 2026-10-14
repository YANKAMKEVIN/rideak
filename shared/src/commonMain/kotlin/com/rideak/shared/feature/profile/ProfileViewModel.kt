package com.rideak.shared.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.PrivacyZoneRepository
import com.rideak.data.RideRepository
import com.rideak.data.location.LocationSourceSelector
import com.rideak.exploration.PrivacySuggestion
import com.rideak.exploration.PrivacyZone
import com.rideak.exploration.suggestPrivacySpots
import com.rideak.shared.di.AppInfo
import com.rideak.shared.di.DeveloperSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val zones: List<PrivacyZone> = emptyList(),
    /** Frequent ride starts/ends not protected yet: most likely home or work. */
    val suggestions: List<PrivacySuggestion> = emptyList(),
)

class ProfileViewModel(
    private val selector: LocationSourceSelector,
    appInfo: AppInfo,
    private val developer: DeveloperSettings,
    rides: RideRepository,
    private val privacy: PrivacyZoneRepository,
) : ViewModel() {
    val showDeveloperTools = appInfo.isDebug
    val simulation: StateFlow<Boolean> = selector.useSimulation

    fun setSimulation(enabled: Boolean) = selector.setUseSimulation(enabled)

    val mapStressTest: StateFlow<Boolean> = developer.mapStressTest
    fun setMapStressTest(enabled: Boolean) { developer.mapStressTest.value = enabled }

    val privacyState: StateFlow<PrivacyUiState> = combine(
        rides.observeFinishedRides().map { rides.finishedEndpoints() },
        privacy.observeZones(),
    ) { endpoints, zones -> PrivacyUiState(zones, suggestPrivacySpots(endpoints, zones)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyUiState())

    fun protect(suggestion: PrivacySuggestion, label: String) {
        viewModelScope.launch { privacy.protect(label, suggestion.position, DEFAULT_RADIUS_METERS) }
    }

    fun remove(zone: PrivacyZone) {
        viewModelScope.launch { privacy.remove(zone.id) }
    }

    private companion object {
        const val DEFAULT_RADIUS_METERS = 400.0
    }
}
