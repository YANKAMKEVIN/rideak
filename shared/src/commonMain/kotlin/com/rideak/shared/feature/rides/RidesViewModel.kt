package com.rideak.shared.feature.rides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideak.data.RideRepository
import com.rideak.ride.RideSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RidesViewModel(repository: RideRepository) : ViewModel() {
    /** Null while loading. */
    val rides: StateFlow<List<RideSummary>?> = repository.observeFinishedRides()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
