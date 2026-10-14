package com.rideak.shared

import com.rideak.data.PrivacyZoneRepository
import com.rideak.exploration.GeoPoint
import com.rideak.exploration.PrivacyZone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory zones for UI tests (no random shift: tests check wiring, not privacy maths). */
class FakePrivacyZoneRepository : PrivacyZoneRepository {
    private val zones = MutableStateFlow<List<PrivacyZone>>(emptyList())
    override fun observeZones(): Flow<List<PrivacyZone>> = zones
    override suspend fun protect(label: String, secret: GeoPoint, radiusMeters: Double) {
        zones.value += PrivacyZone("zone-${zones.value.size}", label, secret, radiusMeters)
    }
    override suspend fun remove(id: String) { zones.value = zones.value.filterNot { it.id == id } }
    override suspend fun removeAll() { zones.value = emptyList() }
}
