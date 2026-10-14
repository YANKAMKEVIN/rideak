package com.rideak.shared.feature.territory

import com.rideak.exploration.CellId
import com.rideak.exploration.ExplorationRegion
import com.rideak.exploration.Place
import com.rideak.exploration.RegionPackCodec
import com.rideak.shared.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Where region packs come from: bundled today, downloaded later (docs/osm-data-strategy.md). */
fun interface RegionSource {
    suspend fun packs(): List<RegionPackCodec.Pack>
}

suspend fun RegionSource.regions(): List<ExplorationRegion> = packs().map { it.region }

suspend fun RegionSource.places(): List<Place> = packs().flatMap { it.places }

/** Packs shipped in `composeResources/files/regions`, decoded once. */
class BundledRegions(private val files: List<String> = listOf("files/regions/cergy.region")) : RegionSource {
    private val mutex = Mutex()
    private var cache: List<RegionPackCodec.Pack>? = null

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun packs(): List<RegionPackCodec.Pack> = mutex.withLock {
        cache ?: files.map { RegionPackCodec.decode(Res.readBytes(it).decodeToString()) }.also { cache = it }
    }
}

/** A named region and how much of its rideable cells the rider has explored. */
data class TerritoryProgress(val name: String, val explored: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else explored.toFloat() / total
}

/**
 * The region to put forward: the one where the rider explored the most rideable cells.
 * None until they have explored at least one, so nobody is shown "Cergy 0 %" from elsewhere.
 */
fun territoryOf(regions: List<ExplorationRegion>, explored: Set<CellId>): TerritoryProgress? =
    regions.map { it to it.progress(explored) }
        .filter { (_, progress) -> progress.exploredRideableCells > 0 }
        .maxByOrNull { (_, progress) -> progress.exploredRideableCells }
        ?.let { (region, progress) -> TerritoryProgress(region.name, progress.exploredRideableCells, progress.totalRideableCells) }
