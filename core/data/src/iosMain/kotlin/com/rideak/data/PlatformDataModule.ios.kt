package com.rideak.data

import androidx.room.Room
import androidx.room.RoomDatabase
import com.rideak.data.db.DATABASE_NAME
import com.rideak.data.db.RideakDatabase
import com.rideak.data.location.LocationSource
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformDataModule: Module = module {
    single<RoomDatabase.Builder<RideakDatabase>> { Room.databaseBuilder<RideakDatabase>(name = databasePath()) }
    single<LocationSource>(DeviceLocation) { IosLocationSource() }
}

@OptIn(ExperimentalForeignApi::class)
private fun databasePath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documents?.path) + "/" + DATABASE_NAME
}
