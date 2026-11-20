package com.myxoz.life.calendar

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.calendar.search.SearchField
import com.myxoz.life.core.plus
import com.myxoz.life.core.roundToNearest15Min
import com.myxoz.life.lifecore.instantevents.InstantEventOpener
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.interfaces.LocationInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours

abstract class CalendarSuper(
    peopleInterface: PeopleInterface,
    locationInterface: LocationInterface
): ViewModel() {
    internal val search = SearchField(peopleInterface, locationInterface, viewModelScope)
    abstract val instantEventOpener: InstantEventOpener
    private val _state = MutableStateFlow<CalendarEventState>(CalendarEventState.NotEditing)
    internal val state = _state.asStateFlow()

    internal fun editExistingEvent(event: SyncedEvent) {
        _state.value = CalendarEventState.Editing.EditingEvent.ExistingEvent.of(event)
    }

    internal fun setEditedSyncableTo(editedSyncable: CalendarEventState.Editing.EditingSyncable){
        _state.value = editedSyncable
    }

    internal fun discardChanges() {
        _state.value = CalendarEventState.NotEditing
    }
    internal fun updateStartTs(ts: Instant) {
        when(val state = _state.value) {
            is CalendarEventState.Editing.EditingEvent -> {
                val rounded = ts.roundToNearest15Min()
                state.raw = state.raw.copyWithTimeline(
                    state.raw.eventTime.copy(
                        start = rounded,
                        end = rounded + state.raw.eventTime.length()
                    )
                )
            }
            is CalendarEventState.Editing.EditingSyncable ->
                state.timestamp = ts
            CalendarEventState.NotEditing ->
                CalendarEventState.Editing.EditingEvent.NewEvent(
                    EmptyEvent(
                        EventTime(
                            ts.roundToNearest15Min(),
                            ts.roundToNearest15Min() + 1.hours,
                            unspecifiedStart = false,
                            unspecifiedLength = false
                        )
                    )
                )
        }
    }

    fun runRecurringHomeTasks() {
        suggestResync()
        refetchAlarmClock()
        autoDetect()
    }

    internal sealed interface CalendarEventState {
        object NotEditing: CalendarEventState
        sealed interface Editing: CalendarEventState {
            sealed class EditingEvent(raw: RawEvent): Editing {
                var raw by mutableStateOf(raw)
                class NewEvent(raw: RawEvent): EditingEvent(raw)
                class ExistingEvent private constructor(
                    private val initialSyncedEvent: SyncedEvent,
                    raw: RawEvent
                ): EditingEvent(raw) {
                    // Remove if unnecessary
                    private val id get() = initialSyncedEvent.id
                    private val created get() =  initialSyncedEvent.created
                    private val edited get() = initialSyncedEvent.edited
                    companion object {
                        fun of(syncedEvent: SyncedEvent) = ExistingEvent(syncedEvent, syncedEvent.raw)
                    }
                }
            }
            sealed interface EditingSyncable: Editing {
                var timestamp: Instant
                sealed class EditingManualTransaction(
                    initialName: String,
                    initialPurpose: String,
                    amountCents: Int,
                    override var timestamp: Instant
                ): EditingSyncable {
                    var name by mutableStateOf(initialName)
                    var purpose by mutableStateOf(initialPurpose)
                    var amountCents by mutableStateOf(abs(amountCents).toString())
                    var isNegative by mutableStateOf(amountCents < 0)

                    class ExistingManualTransaction(val manualTransaction: ManualTransactionSyncable): EditingManualTransaction(
                        manualTransaction.name,
                        manualTransaction.purpose.orEmpty(),
                        manualTransaction.amountCents,
                        manualTransaction.timestamp
                    )
                    class NewManualTransaction(override var timestamp: Instant):
                        EditingManualTransaction("", "", 0, timestamp)
                }
                sealed class EditingTodo(
                    initialShort: String,
                    initialDetails: String,
                    initialTimestamp: Instant
                ): EditingSyncable {
                    override var timestamp by mutableStateOf(initialTimestamp)
                    var short by mutableStateOf(initialShort)
                    var details by mutableStateOf(initialDetails)
                    class ExistingTodo(val todo: TodoSyncable): EditingTodo(
                        todo.short,
                        todo.details.orEmpty(),
                        todo.timestamp
                    )
                    class NewTodo(timestamp: Instant):
                        EditingTodo("", "", timestamp)
                }
            }
        }
    }
    abstract fun suggestResync()
    abstract fun refetchAlarmClock()
    /** TODO Call on (Dispatchers.IO) { */
    abstract fun autoDetect()
}