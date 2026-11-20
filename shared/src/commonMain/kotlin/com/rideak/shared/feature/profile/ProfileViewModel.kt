package com.rideak.shared.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.PrivacyZoneRepository
import com.rideak.data.RideRepository
import com.rideak.data.location.LocationSourceSelector
import com.rideak.exploration.Badge
import com.rideak.exploration.Level
import com.rideak.exploration.PlayerStats
import com.rideak.exploration.PrivacySuggestion
import com.rideak.exploration.PrivacyZone
import com.rideak.exploration.Progression
import com.rideak.exploration.suggestPrivacySpots
import com.rideak.shared.di.AppInfo
import com.rideak.shared.di.DeveloperSettings
import com.rideak.shared.feature.progress.badgeUnlockDates
import com.rideak.shared.feature.progress.playerStats
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProgressUiState(
    val stats: PlayerStats = PlayerStats(),
    val level: Level = Progression.level(0),
    /** When each unlocked badge was earned. */
    val badgeDates: Map<Badge, Long> = emptyMap(),
)

data class PrivacyUiState(
    val zones: List<PrivacyZone> = emptyList(),
    /** Frequent ride starts/ends not protected yet: most likely home or work. */
    val suggestions: List<PrivacySuggestion> = emptyList(),
)

class ProfileViewModel(
    private val selector: LocationSourceSelector,
    appInfo: AppInfo,
    private val developer: DeveloperSettings,
    private val rides: RideRepository,
    private val privacy: PrivacyZoneRepository,
    regions: RegionSource,
    private val settings: AppSettings,
) : ViewModel() {
    val showDeveloperTools = appInfo.isDebug
    val simulation: StateFlow<Boolean> = selector.useSimulation

    fun setSimulation(enabled: Boolean) = selector.setUseSimulation(enabled)

    val keepScreenOn: StateFlow<Boolean> = settings.keepScreenOn
    fun setKeepScreenOn(enabled: Boolean) = settings.setKeepScreenOn(enabled)

    val mapStressTest: StateFlow<Boolean> = developer.mapStressTest
    fun setMapStressTest(enabled: Boolean) { developer.mapStressTest.value = enabled }

    /** Level and badges, derived from every finished ride (nothing stored). */
    val progressState: StateFlow<ProgressUiState> = rides.observeFinishedRides().map { finished ->
        val tracks = finished.map { it to rides.track(it.id) }
        val packs = regions.packs()
        withContext(Dispatchers.Default) {
            val regionList = packs.map { it.region }
            val places = packs.flatMap { it.places }
            val stats = playerStats(finished, tracks.flatMap { it.second }, regionList, places)
            ProgressUiState(stats, Progression.level(Progression.xp(stats)), badgeUnlockDates(tracks, regionList, places))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    val privacyState: StateFlow<PrivacyUiState> = combine(
        rides.observeFinishedRides().map { rides.finishedEndpoints() },
        privacy.observeZones(),
    ) { endpoints, zones -> PrivacyUiState(zones, suggestPrivacySpots(endpoints, zones)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyUiState())

    fun protect(suggestion: PrivacySuggestion, label: String) {
        viewModelScope.launch { privacy.protect(label, suggestion.position, DEFAULT_RADIUS_METERS) }
    }

    /** Right to erasure: finished rides (with points and spots) and privacy zones, on this device. */
    fun eraseAll() {
        viewModelScope.launch {
            rides.deleteFinishedRides()
            privacy.removeAll()
        }
    }

    fun remove(zone: PrivacyZone) {
        viewModelScope.launch { privacy.remove(zone.id) }
    }

    private companion object {
        const val DEFAULT_RADIUS_METERS = 400.0
    }
}
