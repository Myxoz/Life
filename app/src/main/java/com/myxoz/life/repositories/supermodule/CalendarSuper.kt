package com.myxoz.life.repositories.supermodule

import com.myxoz.life.api.Syncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.utils.roundToNearest15Min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CalendarSuper {
    private val _isEditing = MutableStateFlow(false)
    val isEditing = _isEditing.asStateFlow()
    private val _event = MutableStateFlow(SyncedEvent.getSemanticNullValueEvent())
    val event = _event.asStateFlow()
    private val _editedSyncable = MutableStateFlow<Syncable.FeedInstantEventSyncable?>(null)
    val editedSyncable = _editedSyncable.asStateFlow()

    fun setInspectedEventTo(event: SyncedEvent){
        _event.value = event
        _editedSyncable.value = null
    }

    fun setEditedSyncableTo(editedSyncable: Syncable.FeedInstantEventSyncable){
        _isEditing.value = true
        _editedSyncable.value = editedSyncable
    }

    fun setEditing(isNowEditing: Boolean){
        _isEditing.value = isNowEditing
        _editedSyncable.value = null
    }
    fun updateStartTs(ts: Long) {
        if (isEditing.value) {
            val syn = editedSyncable.value
            if(syn == null) {
                val actualTs = ts.roundToNearest15Min()
                _event.value = event.value.copyWithTimes(
                    start = actualTs,
                    end = actualTs + event.value.raw.length()
                )
            } else {
                _editedSyncable.value = syn.copyWithTs(ts)
            }
        } else {
            setEditing(true)
            val actualTs = ts.roundToNearest15Min()
            _event.value = SyncedEvent(
                -1L, 0L, null, EmptyEvent(
                    actualTs,
                    actualTs + 3600 * 1000L,
                    false, usl = false
                )
            )
        }
    }
}