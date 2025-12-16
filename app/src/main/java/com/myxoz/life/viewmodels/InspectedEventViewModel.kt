package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.SyncedEvent
import com.myxoz.life.events.EmptyEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class InspectedEventViewModel: ViewModel() {
    private val _event = MutableStateFlow(SyncedEvent(-1L, 0L, null, EmptyEvent(0L, 0L, false, usl = false)))
    val event = _event.asStateFlow()

    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()
    val popUpToHomeOnEdit = MutableStateFlow(false)
    val timeFlow = flow {
        emit(System.currentTimeMillis())
        while (true){
            delay(1*1000L-System.currentTimeMillis()%(1*1000L))
            emit(System.currentTimeMillis())
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        System.currentTimeMillis()
    )
    fun setInspectedEventTo(event: SyncedEvent){
        _event.value = event
    }

    fun setEditing(isNowEditing: Boolean){
        _isEditing.value = isNowEditing
    }
}