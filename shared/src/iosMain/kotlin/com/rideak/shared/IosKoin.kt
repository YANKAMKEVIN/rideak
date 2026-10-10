package com.rideak.shared

import com.rideak.shared.di.AppInfo
import com.rideak.shared.di.initKoin
import org.koin.dsl.module
import kotlin.experimental.ExperimentalNativeApi

/** Called from iOSApp.swift at launch. */
@OptIn(ExperimentalNativeApi::class)
fun startRideak() = initKoin {
    modules(module { single { AppInfo(isDebug = Platform.isDebugBinary) } })
}
