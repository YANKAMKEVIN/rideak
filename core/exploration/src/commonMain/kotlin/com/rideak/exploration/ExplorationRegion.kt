package com.rideak.exploration

import kotlin.jvm.JvmInline

@JvmInline
value class RegionId(val value: String)

/**
 * A named territory whose progression is measured ("Cergy-Pontoise — 12 %").
 * One ride can contribute to several regions at once (city, agglomeration, département).
 *
 * The denominator is [rideableCells], NOT every cell of the boundary: lakes, railways, motorways
 * and private land must not make 100 % unreachable. Rideable cells will be precomputed per
 * region from geographic data (see docs/osm-data-strategy.md) — not in J0.
 */
data class ExplorationRegion(
    val id: RegionId,
    val name: String,
    val boundary: List<PlaceGeometry.Polygon>,
    /** [CellGrid.id] the [rideableCells] were computed with. */
    val gridId: String,
    val rideableCells: Set<CellId>,
) {
    init {
        require(boundary.isNotEmpty()) { "a region needs a boundary" }
    }

    fun progress(exploredCells: Set<CellId>): ExplorationProgress = ExplorationProgress(
        exploredRideableCells = rideableCells.count { it in exploredCells },
        totalRideableCells = rideableCells.size,
    )
}

data class ExplorationProgress(val exploredRideableCells: Int, val totalRideableCells: Int) {
    val fraction: Double
        get() = if (totalRideableCells == 0) 0.0 else exploredRideableCells.toDouble() / totalRideableCells
}
