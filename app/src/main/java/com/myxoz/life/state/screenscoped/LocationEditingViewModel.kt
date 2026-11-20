package com.myxoz.life.state.screenscoped

import androidx.lifecycle.ViewModel
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.interfaces.LocationInterface

@Deprecated("Use map launched with a specific location instead.")
class LocationEditingViewModel(
    val locationInterface: LocationInterface
): ViewModel()  {
    var nowEditing: LocationSyncable? = null
    suspend fun save(location: LocationSyncable) {
        locationInterface.updateAndStage(location)
    }
}