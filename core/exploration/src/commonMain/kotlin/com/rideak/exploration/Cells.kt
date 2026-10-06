package com.rideak.exploration

import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

/**
 * Opaque identity of one exploration cell. Equality is the only contract: the meaning of
 * [value] belongs to the [CellGrid] that produced it, and the value is prefixed with the grid
 * id so cells from two grids never collide. Swapping the grid strategy never touches the domain.
 */
@JvmInline
value class CellId(val value: String)

/** Strategy that maps a position to the cell containing it. */
interface CellGrid {
    /** Stable identifier, stored alongside cells so a grid change can be detected and recomputed. */
    val id: String

    fun cellAt(point: GeoPoint): CellId
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
