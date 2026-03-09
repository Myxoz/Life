package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.repositories.AppRepositories
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class InspectedEventViewModel(val repos: AppRepositories): ViewModel() {
    private val _event = MutableStateFlow(SyncedEvent.getSemanticNullValueEvent())
    val event = _event.asStateFlow()

    private val _editedSyncable = MutableStateFlow<Syncable.DatedSyncable?>(null)
    val editedSyncable = _editedSyncable.asStateFlow()

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
        _editedSyncable.value = null
    }

    fun updateStartTs(ts: Long){
        if (isEditing.value) {
            val syn = editedSyncable.value
            if(syn == null) {
                _event.value = event.value.copyWithTimes(
                    start = ts,
                    end = ts + event.value.proposed.length()
                )
            } else {
                _editedSyncable.value = syn.copyWithTs(ts)
            }
        } else {
            setEditing(true)
            _event.value = SyncedEvent(
                -1L, 0L, null, EmptyEvent(
                    ts,
                    ts + 3600 * 1000L,
                    false, usl = false
                )
            )
        }
    }

    fun setEditedSyncableTo(editedSyncable: Syncable.DatedSyncable){
        _isEditing.value = true
        _event.value = SyncedEvent.getSemanticNullValueEvent()
        _editedSyncable.value = editedSyncable
    }

    fun setEditing(isNowEditing: Boolean){
        _isEditing.value = isNowEditing
        _editedSyncable.value = null
    }
    suspend fun removeSyncedEvent(event: SyncedEvent) = repos.calendarRepo.removeSyncedEvent(event)
    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent) = repos.calendarRepo.updateOrCreateSyncedEvent(event)
    suspend fun updateOrCreateSynced(syncable: Syncable.DatedSyncable, delete: Boolean = false) {
        if(delete){
            when(syncable){
                is ManualTransactionSyncable -> {
                    repos.bankingRepo.deleteManualTransaction(syncable)
                }
                else -> error("Tried to save syncable, but the syncable is not implemented and therefore can't be saved nor synced")
                // Just using .saveToDb is not possible, because of cache problems.
            }
        } else {
            when(syncable){
                is ManualTransactionSyncable -> {
                    repos.bankingRepo.putManualTransaction(syncable)
                }
                else -> error("Tried to save syncable, but the syncable is not implemented and therefore can't be saved nor synced")
                // Just using .saveToDb is not possible, because of cache problems.
            }
        }
    }
    suspend fun resync() = repos.api.resync()
}