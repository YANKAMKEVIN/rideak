package com.rideak.shared.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.exploration.ExplorationRegion
import com.rideak.shared.feature.territory.RegionSource
import com.rideak.shared.feature.territory.regions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OfflineMapUiState(val regionName: String? = null, val map: OfflineMapState = OfflineMapState.Unavailable)

/** Offline base map of the rider's region (the first bundled region for now). */
class OfflineMapViewModel(regions: RegionSource, private val store: OfflineMapStore) : ViewModel() {
    private val region = MutableStateFlow<ExplorationRegion?>(null)
    private val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<OfflineMapUiState> = combine(
        region.flatMapLatest { r -> if (r == null) flowOf(OfflineMapState.Unavailable) else store.observe(r) },
        region,
        error,
    ) { map, r, failure ->
        OfflineMapUiState(r?.name, failure?.let { OfflineMapState.Failed(it) } ?: map)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfflineMapUiState())

    init {
        viewModelScope.launch { region.value = regions.regions().firstOrNull() }
    }

    fun download() = act { store.download(it) }

    fun delete() = act { store.delete(it) }

    private fun act(action: suspend (ExplorationRegion) -> Unit) {
        val r = region.value ?: return
        error.value = null
        viewModelScope.launch {
            runCatching { action(r) }.onFailure { error.value = it.message ?: "Erreur inconnue" }
        }
    }
}
