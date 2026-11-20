package com.rideak.shared.settings

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private class SharedPreferencesStorage(context: Context) : SettingsStorage {
    private val preferences = context.getSharedPreferences("rideak_settings", Context.MODE_PRIVATE)
    override fun getBoolean(key: String, default: Boolean) = preferences.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
}

actual val platformSettingsModule: Module = module {
    single<SettingsStorage> { SharedPreferencesStorage(androidContext()) }
}
