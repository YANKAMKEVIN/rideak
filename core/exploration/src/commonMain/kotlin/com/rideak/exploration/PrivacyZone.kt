package com.rideak.exploration

/**
 * A sensitive area (home, work…) around which ride data must be protected.
 * Minimal concept for J0: what is recorded, displayed, exported, synced or counted for
 * exploration inside a zone is still to be decided — see docs/privacy.md.
 */
data class PrivacyZone(
    val id: String,
    val label: String,
    val center: GeoPoint,
    val radiusMeters: Double,
) {
    init {
        require(radiusMeters > 0) { "radius must be positive" }
    }

    operator fun contains(point: GeoPoint): Boolean = center.distanceTo(point) <= radiusMeters
}
