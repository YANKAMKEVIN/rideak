package com.rideak.exploration

import kotlin.jvm.JvmInline

@JvmInline
value class PlaceId(val value: String)

enum class PlaceCategory { PARK, FOREST, LAKE, RIVER, VIEWPOINT, MONUMENT, LANDMARK }

/**
 * Shape of a real place. Discovery rules depend on it (future engine):
 * - [Point]: discovered when the ride passes close enough (monument, viewpoint);
 * - [LineString]: discovered when the ride crosses or follows it closely (river);
 * - [Polygon]: discovered when the ride actually enters the area (park, forest).
 */
sealed interface PlaceGeometry {
    data class Point(val position: GeoPoint) : PlaceGeometry

    data class LineString(val points: List<GeoPoint>) : PlaceGeometry {
        init {
            require(points.size >= 2) { "a line needs at least 2 points" }
        }
    }

    /** [exterior] is an open ring (first point not repeated). */
    data class Polygon(
        val exterior: List<GeoPoint>,
        val holes: List<List<GeoPoint>> = emptyList(),
    ) : PlaceGeometry {
        init {
            require(exterior.size >= 3) { "a polygon needs at least 3 points" }
            require(holes.all { it.size >= 3 }) { "every hole needs at least 3 points" }
        }
    }
}

data class Place(
    val id: PlaceId,
    val name: String,
    val category: PlaceCategory,
    val geometry: PlaceGeometry,
)

/** Automatic progression of a place. The ❤️ is deliberately NOT a state here: see [FavoritePlace]. */
enum class PlaceProgress { UNDISCOVERED, DISCOVERED, EXPLORED }

/** When and during which ride a place was discovered. */
data class PlaceDiscovery(
    val placeId: PlaceId,
    val rideId: RideId,
    val discoveredAtEpochMillis: Long,
)

/** A user choice, independent from progression: a place can be discovered without being a favorite. */
data class FavoritePlace(
    val placeId: PlaceId,
    val addedAtEpochMillis: Long,
)
