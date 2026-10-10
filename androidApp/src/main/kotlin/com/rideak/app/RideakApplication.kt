package com.rideak.app

import android.app.Application
import com.rideak.app.ride.AndroidRideForeground
import com.rideak.data.recorder.RideForegroundController
import com.rideak.shared.di.AppInfo
import com.rideak.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class RideakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@RideakApplication)
            modules(
                module {
                    single { AppInfo(isDebug = BuildConfig.DEBUG) }
                    single<RideForegroundController> { AndroidRideForeground(this@RideakApplication) }
                },
            )
        }
    }
}
