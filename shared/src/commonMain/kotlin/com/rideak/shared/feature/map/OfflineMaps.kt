package com.rideak.shared.feature.map

import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.offset
import com.rideak.shared.resources.Res
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.offline.DownloadProgress
import org.maplibre.compose.offline.DownloadStatus
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.offline.OfflineStorageState
import org.maplibre.compose.offline.offlineStorage
import org.maplibre.spatialk.geojson.BoundingBox

/** Offline availability of a region's base map. */
sealed interface OfflineMapState {
    data object Unavailable : OfflineMapState
    data object NotDownloaded : OfflineMapState
    /** [fraction] is null while MapLibre is still counting the tiles to fetch. */
    data class Downloading(val fraction: Float?, val bytes: Long) : OfflineMapState
    data class Ready(val bytes: Long) : OfflineMapState
    data class Failed(val message: String) : OfflineMapState
}

/** Downloads and removes the base map of a region for riding without network. */
interface OfflineMapStore {
    fun observe(region: ExplorationRegion): Flow<OfflineMapState>
    suspend fun download(region: ExplorationRegion)
    suspend fun delete(region: ExplorationRegion)
}

/**
 * MapLibre offline packs. A pack shares the map's own database, so once a region is downloaded the
 * map simply keeps working without network there. OpenFreeMap vector tiles stop at zoom 14 (the
 * map overzooms above), so zooms 10–14 over the region plus 1 km of margin is all a ride needs:
 * a few hundred tiles, not a bulk download.
 */
class MapLibreOfflineMapStore : OfflineMapStore {
    private val storage get() = DefaultMapRuntime.instance.offlineStorage

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(region: ExplorationRegion): Flow<OfflineMapState> =
        storage.state.flatMapLatest { state ->
            val pack = (state as? OfflineStorageState.Ready)?.packs?.firstOrNull { it.belongsTo(region) }
            when {
                state !is OfflineStorageState.Ready -> flowOf(OfflineMapState.Unavailable)
                pack == null -> flowOf(OfflineMapState.NotDownloaded)
                else -> pack.downloadProgress.map(::offlineStateOf)
            }
        }

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun download(region: ExplorationRegion) {
        awaitReady()
        if (packOf(region) != null) return
        val pack = storage.create(
            OfflinePackDefinition.TilePyramid(
                styleUrl = Res.getUri("files/rideak_style.json"),
                bounds = region.downloadBounds(),
                pixelRatio = 2f,
                minZoom = 10.0,
                maxZoom = 14.0,
            ),
            region.id.value.encodeToByteArray(),
        )
        storage.resume(pack)
    }

    override suspend fun delete(region: ExplorationRegion) {
        awaitReady()
        packOf(region)?.let { storage.delete(it) }
    }

    private suspend fun awaitReady() = storage.state.first { it is OfflineStorageState.Ready } as OfflineStorageState.Ready

    private suspend fun packOf(region: ExplorationRegion): OfflinePack? = awaitReady().packs.firstOrNull { it.belongsTo(region) }

    private fun OfflinePack.belongsTo(region: ExplorationRegion) = metadata.value?.decodeToString() == region.id.value
}

/** Region boundary box, 1 km wider on each side (rides start and end just outside the city too). */
internal fun ExplorationRegion.downloadBounds(): BoundingBox {
    val points = boundary.flatMap { it.exterior }
    val southWest = GeoPoint(points.minOf { it.latitude }, points.minOf { it.longitude }).offset(-1_000.0, -1_000.0)
    val northEast = GeoPoint(points.maxOf { it.latitude }, points.maxOf { it.longitude }).offset(1_000.0, 1_000.0)
    return BoundingBox(southWest.longitude, southWest.latitude, northEast.longitude, northEast.latitude)
}

internal fun offlineStateOf(progress: DownloadProgress): OfflineMapState = when (progress) {
    is DownloadProgress.Healthy -> when (progress.status) {
        DownloadStatus.Complete -> OfflineMapState.Ready(progress.completedResourceBytes)
        else -> OfflineMapState.Downloading(
            fraction = progress.requiredResourceCount.takeIf { progress.isRequiredResourceCountPrecise && it > 0 }
                ?.let { (progress.completedResourceCount.toFloat() / it).coerceIn(0f, 1f) },
            bytes = progress.completedResourceBytes,
        )
    }
    is DownloadProgress.Error -> OfflineMapState.Failed(progress.message)
    is DownloadProgress.TileLimitExceeded -> OfflineMapState.Failed("Trop de tuiles (limite ${progress.limit})")
    else -> OfflineMapState.Downloading(fraction = null, bytes = 0)
}
