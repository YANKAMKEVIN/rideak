package com.rideak.shared.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.module.Module

/** Small key-value storage for the rider's settings (SharedPreferences on Android, NSUserDefaults on iOS). */
interface SettingsStorage {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

/** Kept in memory only: tests and previews. */
class InMemorySettingsStorage(initial: Map<String, Boolean> = emptyMap()) : SettingsStorage {
    private val values = initial.toMutableMap()
    override fun getBoolean(key: String, default: Boolean) = values[key] ?: default
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
}

/** Provides the platform [SettingsStorage]. */
expect val platformSettingsModule: Module

/** The rider's settings, persisted on the device. */
class AppSettings(private val storage: SettingsStorage) {
    private val _keepScreenOn = MutableStateFlow(storage.getBoolean(KEEP_SCREEN_ON, default = true))

    /** Keep the screen on while a ride is recording and the ride screen is shown. */
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    fun setKeepScreenOn(enabled: Boolean) {
        storage.putBoolean(KEEP_SCREEN_ON, enabled)
        _keepScreenOn.value = enabled
    }

    private val _onboardingDone = MutableStateFlow(storage.getBoolean(ONBOARDING_DONE, default = false))

    /** The first-launch onboarding was seen (finished or skipped): it is never shown again. */
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    fun setOnboardingDone() {
        storage.putBoolean(ONBOARDING_DONE, true)
        _onboardingDone.value = true
    }

    companion object {
        const val KEEP_SCREEN_ON = "keep_screen_on"
        const val ONBOARDING_DONE = "onboarding_done"
    }
}
