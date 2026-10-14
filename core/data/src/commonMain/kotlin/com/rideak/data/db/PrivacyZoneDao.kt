package com.rideak.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacyZoneDao {
    @Insert
    suspend fun insert(zone: PrivacyZoneEntity)

    @Query("DELETE FROM privacy_zones WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM privacy_zones")
    suspend fun deleteAll()

    @Query("SELECT * FROM privacy_zones ORDER BY createdAt")
    fun observeAll(): Flow<List<PrivacyZoneEntity>>
}
