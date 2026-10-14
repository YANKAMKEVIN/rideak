package com.rideak.shared

import com.rideak.exploration.ExplorationRegion
import com.rideak.shared.feature.map.OfflineMapState
import com.rideak.shared.feature.map.OfflineMapStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** No MapLibre runtime in UI tests: downloads complete instantly. */
class FakeOfflineMapStore : OfflineMapStore {
    private val state = MutableStateFlow<OfflineMapState>(OfflineMapState.NotDownloaded)
    override fun observe(region: ExplorationRegion): Flow<OfflineMapState> = state
    override suspend fun download(region: ExplorationRegion) { state.value = OfflineMapState.Ready(4_200_000) }
    override suspend fun delete(region: ExplorationRegion) { state.value = OfflineMapState.NotDownloaded }
}
