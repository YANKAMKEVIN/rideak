package com.rideak.data

import androidx.room.RoomDatabase
import com.rideak.data.db.RideakDatabase
import com.rideak.data.db.buildRideakDatabase
import com.rideak.data.location.LocationSource
import com.rideak.data.location.LocationSourceSelector
import com.rideak.data.location.SimulatedLocationSource
import com.rideak.data.recorder.NoForegroundController
import com.rideak.data.recorder.RideForegroundController
import com.rideak.data.recorder.RideRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

val DeviceLocation = named("deviceLocation")
val ApplicationScope = named("applicationScope")

@OptIn(ExperimentalTime::class)
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

@OptIn(ExperimentalUuidApi::class)
fun newUuid(): String = Uuid.random().toString()

/** Platform pieces: database builder and device [LocationSource] (qualified [DeviceLocation]). */
expect val platformDataModule: Module

val dataModule = module {
    single<CoroutineScope>(ApplicationScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { get<RoomDatabase.Builder<RideakDatabase>>().buildRideakDatabase() }
    single { get<RideakDatabase>().rideDao() }
    single<RideRepository> { RoomRideRepository(get()) }
    single { get<RideakDatabase>().privacyZoneDao() }
    single<PrivacyZoneRepository> { RoomPrivacyZoneRepository(get(), ::nowEpochMillis, ::newUuid) }
    single { LocationSourceSelector(device = get<LocationSource>(DeviceLocation), simulated = SimulatedLocationSource(::nowEpochMillis)) }
    single {
        val selector = get<LocationSourceSelector>()
        RideRecorder(
            repository = get(),
            locationSource = selector::current,
            scope = get(ApplicationScope),
            clock = ::nowEpochMillis,
            newId = ::newUuid,
            foreground = getOrNull<RideForegroundController>() ?: NoForegroundController,
        )
    }
}
