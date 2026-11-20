package com.rideak.shared.settings

import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

private class UserDefaultsStorage : SettingsStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    override fun getBoolean(key: String, default: Boolean) =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)
    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, forKey = key)
}

actual val platformSettingsModule: Module = module {
    single<SettingsStorage> { UserDefaultsStorage() }
}
