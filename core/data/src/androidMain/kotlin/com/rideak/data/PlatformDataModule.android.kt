package com.rideak.data

import androidx.room.Room
import androidx.room.RoomDatabase
import com.rideak.data.db.DATABASE_NAME
import com.rideak.data.db.RideakDatabase
import com.rideak.data.location.LocationSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<RoomDatabase.Builder<RideakDatabase>> {
        val context = androidContext()
        Room.databaseBuilder<RideakDatabase>(context, context.getDatabasePath(DATABASE_NAME).absolutePath)
    }
    single<LocationSource>(DeviceLocation) { FusedLocationSource(androidContext()) }
}
