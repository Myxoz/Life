package com.myxoz.life.ui.feed.fullscreenevent

import android.icu.util.Calendar
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.API
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.repositories.supermodule.CalendarSuper
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class InspectedEventViewModel(
    savedStateHandle: SavedStateHandle,
    private val dbInterface: DatabaseInterface,
    private val calendarSuper: CalendarSuper
): ViewModel() {
    val eventId = savedStateHandle.get<Long>(NavPath.FULLSCREEN_EVENT.parameterName).takeIf { (it ?: 0) > 0 }
    val showRepeatPopup = MutableStateFlow(false)
    val timeFlow = flow {
        emit(System.currentTimeMillis())
        while (true) {
            delay(1 * 1000L - System.currentTimeMillis() % (1 * 1000L))
            emit(System.currentTimeMillis())
        }
    }
    val repeatingEvents = dbInterface.extensionInterface.flowFor(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents)
    suspend fun updateRepeatingEvent(event: SyncedEvent, type: RepeatingEventsExtendable.RepeatType?, oldId: Long?) {
        dbInterface.extensionInterface.updateAndSyncWith(ExtensionSyncable.ExtensionSyncableType.RepeatingEvents) { old ->
            if(type == null) {
                val old = old.value ?: return
                val res = RepeatingEventsExtendable(
                    old.events.mapNotNull {
                        if (it.id == oldId) null else it
                    }
                )
                dbInterface.hooks.updateRepeatingEvents(res)
                return@updateAndSyncWith res
            }
            val calendar = Calendar.getInstance()
            val oldItem = old.value?.events?.find { it.id == oldId }
            val self = if(oldId == null || oldItem == null)
                RepeatingEventsExtendable.RepeatingEvent(
                    API.Companion.generateId(),
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
                    old.value.events.map { if (it.id == oldId) self else it }
                )
            } else {
                RepeatingEventsExtendable(
                    old.value.events + listOf(self)
                )
            }
            dbInterface.hooks.updateRepeatingEvents(res)
            return@updateAndSyncWith res
        }
    }

    suspend fun removeSyncedEvent(event: SyncedEvent) = dbInterface.calendarInterface.removeSyncedEvent(event)
    suspend fun updateOrCreateSyncedEvent(event: SyncedEvent, wasEdited: Boolean) = dbInterface.calendarInterface.updateOrCreateSyncedEvent(event, wasEdited)
    suspend fun updateOrCreateSynced(syncable: Syncable.FeedInstantEventSyncable, delete: Boolean = false) {
        if(delete){
            syncable.delete(dbInterface)
        } else {
            syncable.saveWithCache(dbInterface)
        }
    }
    suspend fun resync() = dbInterface.api.resync()

    // Propagated
    val isEditing = calendarSuper.isEditing
    val semanticNullValueEvent = SyncedEvent(-1L, 0L, null, EmptyEvent(0L, 0L, false, usl = false))
    @OptIn(ExperimentalCoroutinesApi::class)
    val event =
        (
            if(eventId != null && !isEditing.value)
                isEditing.flatMapMerge { isEditing ->
                    if(isEditing) calendarSuper.event else dbInterface.calendarInterface.eventById(eventId).map { it?.value ?: semanticNullValueEvent }
                }.subscribeToColdFlow(viewModelScope, semanticNullValueEvent)
            else
                calendarSuper.event
        )
    val editedSyncable = calendarSuper.editedSyncable
    fun setInspectedEventTo(event: SyncedEvent) = calendarSuper.setInspectedEventTo(event)
    fun setEditedSyncableTo(editedSyncable: Syncable.FeedInstantEventSyncable) = calendarSuper.setEditedSyncableTo(editedSyncable)
    fun setEditing(isNowEditing: Boolean) = calendarSuper.setEditing(isNowEditing)
    fun updateStartTs(ts: Long) = calendarSuper.updateStartTs(ts)
}