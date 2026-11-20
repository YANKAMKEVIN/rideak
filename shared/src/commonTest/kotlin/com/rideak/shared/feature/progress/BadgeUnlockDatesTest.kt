package com.rideak.shared.feature.progress

import com.rideak.exploration.Badge
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.RideId
import com.rideak.exploration.TrackPoint
import com.rideak.ride.RideStatus
import com.rideak.ride.RideSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BadgeUnlockDatesTest {
    private fun ride(id: String, start: Long, km: Double) =
        RideSummary(RideId(id), start, start + 3_600_000, RideStatus.FINISHED, 3_600_000, km * 1000, 3_600_000, 6.0, 0.0)

    private val track = listOf(listOf(TrackPoint(GeoPoint(49.0364, 2.0761), 0L)))

    @Test
    fun eachBadgeIsDatedByTheRideThatUnlockedIt() {
        // Given in any order: replayed from the oldest.
        val dates = badgeUnlockDates(
            listOf(ride("b", 10_000_000, 6.0) to track, ride("a", 0, 5.0) to track),
            regions = emptyList(),
            places = emptyList(),
        )
        assertEquals(3_600_000L, dates[Badge.FIRST_RIDE])
        assertEquals(13_600_000L, dates[Badge.TEN_KM], "5 + 6 km: reached at the end of the second ride")
        assertNull(dates[Badge.HUNDRED_KM])
    }
}
