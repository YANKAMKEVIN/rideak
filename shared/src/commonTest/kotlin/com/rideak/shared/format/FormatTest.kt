package com.rideak.shared.format

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test
    fun kilometersUseAFrenchDecimalComma() {
        assertEquals("8,42", formatKilometers(8_420.0))
        assertEquals("0,05", formatKilometers(49.6))
        assertEquals("147", formatKilometersShort(147_400.0))
        assertEquals("12,3", formatKilometersShort(12_340.0))
    }

    @Test
    fun durations() {
        assertEquals("00:34:12", formatDuration(2_052_000))
        assertEquals("1 h 04", formatDurationShort(3_840_000))
        assertEquals("34 min", formatDurationShort(2_052_000))
        assertEquals("19 s", formatDurationShort(19_400))
    }

    @Test
    fun speeds() {
        assertEquals("21,6", formatKmh(6.0))
        assertEquals(23, speedKmhRounded(6.4))
    }
}
