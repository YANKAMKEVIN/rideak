package com.rideak.data.recorder

import com.rideak.exploration.TrackPoint

/**
 * The positions of the ride being recorded, published to the screen once per second and at every fix.
 * Copying the whole track each time costs more as the ride grows (a three-hour ride holds about ten
 * thousand points). Here an append writes past the end of the shared array, where no published
 * [snapshot] reads, so a snapshot costs nothing and never changes afterwards. The array is copied only
 * when it is full, doubling its size. Not thread-safe: the recorder appends under its mutex.
 */
internal class GrowingTrack {
    private var points = arrayOfNulls<TrackPoint>(INITIAL_CAPACITY)
    private var size = 0
    private var published: List<TrackPoint> = emptyList()

    fun add(point: TrackPoint) {
        if (size == points.size) points = points.copyOf(points.size * 2)
        points[size++] = point
    }

    fun addAll(track: Iterable<TrackPoint>) = track.forEach(::add)

    /** An immutable view of the positions so far; the same instance until the next [add]. */
    fun snapshot(): List<TrackPoint> {
        if (published.size != size) published = Snapshot(points, size)
        return published
    }

    private class Snapshot(private val points: Array<TrackPoint?>, override val size: Int) : AbstractList<TrackPoint>() {
        override fun get(index: Int): TrackPoint {
            if (index !in 0 until size) throw IndexOutOfBoundsException("index $index, size $size")
            return points[index]!!
        }
    }

    private companion object {
        const val INITIAL_CAPACITY = 256
    }
}
