package com.rideak.data.recorder

import com.rideak.exploration.GeoPoint
import com.rideak.exploration.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GrowingTrackTest {
    private fun point(i: Int) = TrackPoint(GeoPoint(49.0, 2.0 + i * 0.0001), i * 1_000L)

    @Test
    fun aSnapshotKeepsItsPointsWhileTheTrackGrows() {
        val track = GrowingTrack()
        assertEquals(emptyList(), track.snapshot())
        (0 until 3).forEach { track.add(point(it)) }
        val early = track.snapshot()
        // Well past the first array: several copies happen meanwhile.
        (3 until 1_000).forEach { track.add(point(it)) }
        val late = track.snapshot()

        assertEquals((0 until 3).map(::point), early)
        assertEquals((0 until 1_000).map(::point), late)
        assertFailsWith<IndexOutOfBoundsException> { early[3] }
    }

    @Test
    fun theSameSnapshotIsReturnedUntilAPointIsAdded() {
        val track = GrowingTrack()
        track.addAll(listOf(point(0), point(1)))
        val first = track.snapshot()
        assertSame(first, track.snapshot())
        track.add(point(2))
        val second = track.snapshot()
        assertEquals(3, second.size)
        assertEquals(2, first.size)
        assertEquals(listOf(point(0), point(1), point(2)), second)
    }
}
