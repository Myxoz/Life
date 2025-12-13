package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharingViewModel: ViewModel() {
    private var _entry = MutableStateFlow<SharingContent?>(null)
    val entry = _entry.asStateFlow()
    fun setSharedContent(content: SharingContent){
        _entry.value = content
    }
}
abstract class SharingContent