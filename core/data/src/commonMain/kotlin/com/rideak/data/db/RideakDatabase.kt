package com.rideak.data.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [RideEntity::class, TrackPointEntity::class, SpotEntity::class, PrivacyZoneEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@ConstructedBy(RideakDatabaseConstructor::class)
abstract class RideakDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun privacyZoneDao(): PrivacyZoneDao
}

// Implemented by the Room compiler for each platform.
@Suppress("KotlinNoActualForExpect")
expect object RideakDatabaseConstructor : RoomDatabaseConstructor<RideakDatabase> {
    override fun initialize(): RideakDatabase
}

internal const val DATABASE_NAME = "rideak.db"

/** Same SQLite (bundled) and threading on every platform. */
fun RoomDatabase.Builder<RideakDatabase>.buildRideakDatabase(): RideakDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
