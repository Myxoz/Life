package com.myxoz.life.viewmodels

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import com.myxoz.life.api.API
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.utils.roundToNearest15Min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

class InspectedEventViewModel(val repos: AppRepositories): ViewModel() {
    private val _event = MutableStateFlow(SyncedEvent.getSemanticNullValueEvent())
    val event = _event.asStateFlow()

    val showRepeatPopup = MutableStateFlow(false)

    private val _editedSyncable = MutableStateFlow<Syncable.FeedInstantEventSyncable?>(null)
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
    }
    fun setInspectedEventTo(event: SyncedEvent){
        _event.value = event
        _editedSyncable.value = null
    }
    val repeatingEvents = repos.extensionRepo.flowFor(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents)
    suspend fun updateRepeatingEvent(event: SyncedEvent, type: RepeatingEventsExtendable.RepeatType?, oldId: Long?) {
        repos.extensionRepo.updateAndSyncWith(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents) { old ->
            if(type == null) {
                val old = old.value ?: return
                val res = RepeatingEventsExtendable(
                    old.events.mapNotNull {
                        if(it.id == oldId) null else it
                    }
                )
                repos.hooks.updateRepeatingEvents(res)
                return@updateAndSyncWith res
            }
            val calendar = Calendar.getInstance()
            val oldItem = old.value?.events?.find { it.id == oldId }
            val self = if(oldId == null || oldItem == null)
                RepeatingEventsExtendable.RepeatingEvent(
                    API.generateId(),
                    type,
                    event.raw.copyWithTimes(
                        type.getNext(calendar, event.raw.start),
                        type.getNext(calendar, event.raw.end),
                    ),
                    listOf(event.id)
                )
            else
                oldItem

            val res = if(old.value == null)
                RepeatingEventsExtendable(listOf(self))
            else if(oldItem != null) {
                RepeatingEventsExtendable(
                    old.value.events.map { if(it.id == oldId) self else it }
                )
            } else {
                RepeatingEventsExtendable(
                    old.value.events + listOf(self)
                )
            }
            repos.hooks.updateRepeatingEvents(res)
            return@updateAndSyncWith res
        }
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

    fun setEditedSyncableTo(editedSyncable: Syncable.FeedInstantEventSyncable){
        _isEditing.value = true
        _editedSyncable.value = editedSyncable
    }

    fun setEditing(isNowEditing: Boolean){
        _isEditing.value = isNowEditing
        _editedSyncable.value = null
    }
    suspend fun removeSyncedEvent(event: SyncedEvent) = repos.calendarRepo.removeSyncedEvent(event)
    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent, wasEdited: Boolean) = repos.calendarRepo.updateOrCreateSyncedEvent(event, wasEdited)
    suspend fun updateOrCreateSynced(syncable: Syncable.FeedInstantEventSyncable, delete: Boolean = false) {
        if(delete){
            syncable.delete(repos)
        } else {
            syncable.saveWithCache(repos)
        }
    }
    suspend fun resync() = repos.api.resync()
}