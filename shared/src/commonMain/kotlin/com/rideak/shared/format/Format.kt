package com.rideak.shared.format

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// French formatting of ride values ("8,42 km", "00:34:12", "Sortie du 11 octobre").

fun formatKilometers(meters: Double): String {
    val hundredths = (meters / 10).roundToLong()
    return "${hundredths / 100},${(hundredths % 100).toString().padStart(2, '0')}"
}

/** Home totals: "147" from 100 km, "12,3" below. */
fun formatKilometersShort(meters: Double): String {
    val km = meters / 1000
    if (km >= 100) return km.roundToInt().toString()
    val tenths = (km * 10).roundToInt()
    return "${tenths / 10},${tenths % 10}"
}

/** Storage size: "12,3 Mo", "850 ko". */
fun formatBytes(bytes: Long): String {
    if (bytes < 1_000_000) return "${(bytes / 1_000.0).roundToInt()} ko"
    val tenths = (bytes / 100_000.0).roundToInt()
    return "${tenths / 10},${tenths % 10} Mo"
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "${h.pad()}:${m.pad()}:${s.pad()}"
}

/** "1 h 04" / "34 min" / "19 s": compact form for summaries. */
fun formatDurationShort(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes >= 60 -> "${minutes / 60} h ${(minutes % 60).pad()}"
        minutes >= 1 -> "$minutes min"
        else -> "${millis / 1000} s"
    }
}

fun formatKmh(metersPerSecond: Double): String {
    val tenths = (metersPerSecond * 36).roundToInt()
    return "${tenths / 10},${tenths % 10}"
}

fun speedKmhRounded(metersPerSecond: Double): Int = (metersPerSecond * 3.6).roundToInt()

private val months = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

@OptIn(ExperimentalTime::class)
private fun local(epochMillis: Long) =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())

/** "Octobre 2026": month headers of the ride list. */
fun formatMonth(epochMillis: Long): String {
    val date = local(epochMillis)
    return "${months[date.month.ordinal].replaceFirstChar { it.uppercase() }} ${date.year}"
}

/** "11 octobre". */
fun formatDay(epochMillis: Long): String {
    val date = local(epochMillis)
    return "${date.day} ${months[date.month.ordinal]}"
}

fun formatRideTitle(startedAt: Long): String {
    val date = local(startedAt)
    return "Sortie du ${date.day} ${months[date.month.ordinal]}"
}

fun formatClock(epochMillis: Long): String {
    val time = local(epochMillis)
    return "${time.hour}h${time.minute.pad()}"
}

private fun Number.pad() = toString().padStart(2, '0')
