package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.repositories.LocationRepo

class LocationEditingViewModel(
    val locationRepo: LocationRepo
): ViewModel()  {
    var nowEditing: LocationSyncable? = null
    suspend fun save(location: LocationSyncable) {
        locationRepo.updateAndStage(location)
    }
}