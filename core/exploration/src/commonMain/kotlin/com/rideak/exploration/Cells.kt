package com.rideak.exploration

import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Opaque identity of one exploration cell. Equality is the only contract: the meaning of
 * [value] belongs to the [CellGrid] that produced it, and the value is prefixed with the grid
 * id so cells from two grids never collide. Swapping the grid strategy never touches the domain.
 */
@JvmInline
value class CellId(val value: String)

/** Axis-aligned geographic rectangle. */
data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double)

/** Strategy that maps a position to the cell containing it. */
interface CellGrid {
    /** Stable identifier, stored alongside cells so a grid change can be detected and recomputed. */
    val id: String

    fun cellAt(point: GeoPoint): CellId

    /** Geographic extent of a cell of this grid, for drawing; null if [cell] belongs to another grid. */
    fun boundsOf(cell: CellId): GeoBounds?
}

/**
 * V1 grid: square Web-Mercator ("slippy map") tiles. Pure arithmetic, aligned with map tiles.
 * At zoom 17 a cell is ~200 m wide around Paris (latitude 49°).
 */
class SlippyTileGrid(val zoom: Int = 17) : CellGrid {
    init {
        require(zoom in 1..22) { "unsupported zoom: $zoom" }
    }

    override val id: String = "tile$zoom"

    override fun cellAt(point: GeoPoint): CellId {
        val n = 1 shl zoom
        val lat = point.latitude.coerceIn(-MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE).toRadians()
        val x = floor((point.longitude + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val y = floor((1.0 - asinh(tan(lat)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
        return CellId("$id/$x/$y")
    }

    override fun boundsOf(cell: CellId): GeoBounds? {
        val parts = cell.value.split('/')
        if (parts.size != 3 || parts[0] != id) return null
        val x = parts[1].toIntOrNull() ?: return null
        val y = parts[2].toIntOrNull() ?: return null
        val n = (1 shl zoom).toDouble()
        fun lat(row: Int) = atan(sinh(PI * (1 - 2 * row / n))) * 180 / PI
        return GeoBounds(south = lat(y + 1), west = x / n * 360 - 180, north = lat(y), east = (x + 1) / n * 360 - 180)
    }

    private companion object {
        const val MAX_MERCATOR_LATITUDE = 85.05112878
    }
}

/** Automatic progression of a cell. Never carries a user choice. */
enum class CellProgress { UNKNOWN, TRAVERSED, EXPLORED }

/**
 * What the user has done in one cell. Progress is not stored: it is derived from these facts by
 * rules that will evolve (e.g. TRAVERSED -> EXPLORED after several visits or neighbour density).
 */
data class UserCell(
    val cellId: CellId,
    val firstRideId: RideId,
    val firstSeenAtEpochMillis: Long,
    val visitCount: Int,
)
