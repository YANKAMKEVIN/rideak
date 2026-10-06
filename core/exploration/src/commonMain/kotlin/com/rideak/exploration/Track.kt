package com.rideak.exploration

import kotlin.jvm.JvmInline

@JvmInline
value class RideId(val value: String)

/**
 * One raw GPS sample. TrackPoints are the source of truth: exploration (cells, discoveries)
 * is always *derived* from them, so it can be recomputed when the grid, rules or thresholds change.
 * Keep raw sensor values here — no filtering or snapping at capture time.
 */
data class TrackPoint(
    val position: GeoPoint,
    val timestampEpochMillis: Long,
    /** Horizontal accuracy radius (68 % confidence) as reported by the OS, if known. */
    val horizontalAccuracyMeters: Float? = null,
    val altitudeMeters: Double? = null,
    /** Speed reported by the OS, if known. Never used as a reward signal. */
    val speedMetersPerSecond: Float? = null,
)
