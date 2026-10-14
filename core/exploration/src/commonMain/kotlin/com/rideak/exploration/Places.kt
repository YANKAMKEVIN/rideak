package com.rideak.exploration

import kotlin.jvm.JvmInline

@JvmInline
value class PlaceId(val value: String)

enum class PlaceCategory { PARK, FOREST, LAKE, RIVER, VIEWPOINT, MONUMENT, LANDMARK }

/**
 * Shape of a real place. Discovery rules depend on it (future engine):
 * - [Point]: discovered when the ride passes close enough (monument, viewpoint);
 * - [LineString] / [MultiLineString]: discovered when the ride crosses or follows it closely (river);
 * - [Polygon] / [MultiPolygon]: discovered when the ride actually enters the area (park, forest),
 *   or rides along its shore for water you cannot ride into (lake).
 */
sealed interface PlaceGeometry {
    data class Point(val position: GeoPoint) : PlaceGeometry

    data class LineString(val points: List<GeoPoint>) : PlaceGeometry {
        init {
            require(points.size >= 2) { "a line needs at least 2 points" }
        }
    }

    /** One place drawn as several lines (a river split into many OSM ways). */
    data class MultiLineString(val lines: List<LineString>) : PlaceGeometry {
        init {
            require(lines.isNotEmpty()) { "a multi-line needs at least one line" }
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

    /** One place made of several areas (a park split in two by a road). */
    data class MultiPolygon(val polygons: List<Polygon>) : PlaceGeometry {
        init {
            require(polygons.isNotEmpty()) { "a multi-polygon needs at least one polygon" }
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
