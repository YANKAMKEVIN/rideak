package com.rideak.shared.feature.ride

import com.rideak.data.recorder.RecorderState

/**
 * Text of the big "PARTIR À L'AVENTURE" button, wherever it opens the ride screen: once a ride is
 * under way, that screen shows the ride in progress instead of starting a new one.
 */
fun rideButtonLabel(active: RecorderState.Active?): String = when {
    active == null -> "PARTIR\nÀ L'AVENTURE"
    active.recovered -> "SORTIE\nINTERROMPUE"
    else -> "RETOUR\nÀ LA SORTIE"
}
