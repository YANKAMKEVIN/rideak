package com.rideak.data

import com.rideak.data.db.PrivacyZoneDao
import com.rideak.data.db.PrivacyZoneEntity
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PrivacyZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The rider's privacy zones, on the device only. */
interface PrivacyZoneRepository {
    fun observeZones(): Flow<List<PrivacyZone>>

    /** Protects [secret] with a shifted zone (PrivacyZone.around); [secret] itself is not stored. */
    suspend fun protect(label: String, secret: GeoPoint, radiusMeters: Double)

    suspend fun remove(id: String)
}

class RoomPrivacyZoneRepository(
    private val dao: PrivacyZoneDao,
    private val clock: () -> Long,
    private val newId: () -> String,
) : PrivacyZoneRepository {
    override fun observeZones() = dao.observeAll().map { rows ->
        rows.map { PrivacyZone(it.id, it.label, GeoPoint(it.latitude, it.longitude), it.radiusM) }
    }

    override suspend fun protect(label: String, secret: GeoPoint, radiusMeters: Double) {
        val zone = PrivacyZone.around(newId(), label, secret, radiusMeters)
        dao.insert(PrivacyZoneEntity(zone.id, zone.label, zone.center.latitude, zone.center.longitude, zone.radiusMeters, clock()))
    }

    override suspend fun remove(id: String) = dao.delete(id)
}
