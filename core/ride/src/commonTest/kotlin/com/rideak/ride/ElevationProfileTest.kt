package com.rideak.ride

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ElevationProfileTest {
    @Test
    fun followsDistanceAndAltitude() {
        val track = Traces.straight(seconds = 100, speedMps = 5.0, altitude = { s -> 50.0 + s })
        val profile = elevationProfile(listOf(track))
        assertEquals(101, profile.size)
        assertEquals(50.0, profile.first().altitudeMeters)
        assertTrue(profile.last().distanceMeters in 495.0..505.0)
    }

    @Test
    fun pointsWithoutAltitudeAreSkipped() {
        assertTrue(elevationProfile(listOf(Traces.straight(seconds = 10, speedMps = 5.0))).isEmpty())
    }

    @Test
    fun longRidesAreDownsampled() {
        val track = Traces.straight(seconds = 5_000, speedMps = 5.0, altitude = { 10.0 })
        assertTrue(elevationProfile(listOf(track), maxPoints = 200).size <= 201)
    }
}
