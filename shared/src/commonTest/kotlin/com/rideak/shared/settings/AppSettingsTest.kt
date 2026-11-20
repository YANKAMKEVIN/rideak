package com.rideak.shared.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsTest {
    @Test
    fun screenStaysOnByDefault() {
        assertTrue(AppSettings(InMemorySettingsStorage()).keepScreenOn.value)
    }

    @Test
    fun turningKeepScreenOnOffIsRememberedAcrossLaunches() {
        val storage = InMemorySettingsStorage()
        AppSettings(storage).setKeepScreenOn(false)
        assertFalse(AppSettings(storage).keepScreenOn.value)
    }
}
