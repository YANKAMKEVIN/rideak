package com.rideak.data

import androidx.room.Room
import androidx.room.RoomDatabase
import com.rideak.data.db.DATABASE_NAME
import com.rideak.data.db.RideakDatabase
import com.rideak.data.location.LocationSource
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

// The jvm target only exists for tests: no real GPS.
actual val platformDataModule: Module = module {
    single<RoomDatabase.Builder<RideakDatabase>> {
        Room.databaseBuilder<RideakDatabase>(File(System.getProperty("java.io.tmpdir"), DATABASE_NAME).absolutePath)
    }
    single<LocationSource>(DeviceLocation) { LocationSource { emptyFlow() } }
}
