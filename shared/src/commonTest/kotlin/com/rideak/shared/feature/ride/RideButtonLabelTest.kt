package com.rideak.shared.feature.ride

import com.rideak.data.recorder.GpsSignal
import com.rideak.data.recorder.RecorderState
import com.rideak.exploration.RideId
import com.rideak.ride.RideMetrics
import com.rideak.ride.RideStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class RideButtonLabelTest {
    private fun active(recovered: Boolean) = RecorderState.Active(
        rideId = RideId("r"),
        status = if (recovered) RideStatus.PAUSED else RideStatus.RECORDING,
        startedAtEpochMillis = 0L,
        activeDurationMillis = 0L,
        metrics = RideMetrics(),
        signal = GpsSignal.GOOD,
        spotCount = 0,
        recovered = recovered,
    )

    @Test
    fun theButtonSaysWhereItLeads() {
        assertEquals("PARTIR\nÀ L'AVENTURE", rideButtonLabel(null))
        assertEquals("RETOUR\nÀ LA SORTIE", rideButtonLabel(active(recovered = false)))
        assertEquals("SORTIE\nINTERROMPUE", rideButtonLabel(active(recovered = true)))
    }
}
