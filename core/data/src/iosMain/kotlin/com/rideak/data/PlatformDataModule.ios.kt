package com.rideak.data

import androidx.room.Room
import androidx.room.RoomDatabase
import com.rideak.data.db.DATABASE_NAME
import com.rideak.data.db.RideakDatabase
import com.rideak.data.location.LocationSource
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask

actual val platformDataModule: Module = module {
    single<RoomDatabase.Builder<RideakDatabase>> { Room.databaseBuilder<RideakDatabase>(name = databasePath()) }
    single<LocationSource>(DeviceLocation) { IosLocationSource() }
}

/**
 * The database lives in Application Support/RideakData, a folder excluded from iCloud and computer
 * backups (docs/privacy.md: no system backup). Builds up to J1 kept it in Documents, which is backed
 * up: the files are moved once, before Room opens them.
 */
@OptIn(ExperimentalForeignApi::class)
private fun databasePath(): String {
    val files = NSFileManager.defaultManager
    fun directory(kind: NSSearchPathDirectory) =
        requireNotNull(files.URLForDirectory(kind, NSUserDomainMask, appropriateForURL = null, create = true, error = null))
    val data = requireNotNull(directory(NSApplicationSupportDirectory).URLByAppendingPathComponent("RideakData", isDirectory = true))
    files.createDirectoryAtURL(data, withIntermediateDirectories = true, attributes = null, error = null)
    data.setResourceValue(true, forKey = NSURLIsExcludedFromBackupKey, error = null)

    val newPath = requireNotNull(data.path) + "/" + DATABASE_NAME
    val oldPath = requireNotNull(directory(NSDocumentDirectory).path) + "/" + DATABASE_NAME
    if (files.fileExistsAtPath(oldPath) && !files.fileExistsAtPath(newPath)) {
        listOf("", "-wal", "-shm").forEach { suffix ->
            if (files.fileExistsAtPath(oldPath + suffix)) files.moveItemAtPath(oldPath + suffix, toPath = newPath + suffix, error = null)
        }
        files.removeItemAtPath("$oldPath.lck", error = null) // Room's empty lock file, recreated next to the database
    }
    return newPath
}
