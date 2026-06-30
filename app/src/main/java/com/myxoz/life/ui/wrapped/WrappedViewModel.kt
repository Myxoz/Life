package com.myxoz.life.ui.wrapped

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.repositories.AppRepos
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow

class WrappedViewModel(
    val dbInterface: DatabaseInterface
): ViewModel() {
    private val profilePictureCache = StateFlowCache<Long, Bitmap?>{ personId ->
        dbInterface.aggregators.peopleAggregator.getProfilePicture(personId).subscribeToColdFlow(viewModelScope, null)
    }
    fun getProfilePicture(personId: Long) = profilePictureCache.get(personId)
}