package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent

internal object SyncableIds {
    fun Syncable.getCalendarId(): Int =
        when(this) {
            is BankingSidecarSyncable -> BANKINGSIDECAR
            is DeleteEntry -> deleteCalendarId
            is ExtensionSyncable -> EXTENSION
            is FullDaySyncable -> DAYS
            is LocationSyncable -> LOCATIONS
            is ManualTransactionSyncable -> MANUALTRANSACTION
            is PersonSyncable -> PEOPLE
            is ProfilePictureSyncable -> PROFILEPICTURE
            is SyncedEvent -> raw.getEventCalendarId()
            is TodoSyncable -> TODOS
            is TransactionSplitSyncable -> TRANSACTIONSPLIT
        }
    fun RawEvent.getEventCalendarId(): Int =
        when(this) {
            is EmptyEvent -> EmptyEvent.CALENDARID
            is SleepEvent -> SleepEvent.CALENDARID
            is SpontEvent -> SpontEvent.CALENDARID
            is HobbyEvent -> HobbyEvent.CALENDARID
            is LearnEvent -> LearnEvent.CALENDARID
            is SocialEvent -> SocialEvent.CALENDARID
            is TravelEvent -> TravelEvent.CALENDARID
            is DigSocEvent -> DigSocEvent.CALENDARID
            is WorkEvent -> WorkEvent.CALENDARID
            is TimewasteEvent -> TimewasteEvent.CALENDARID
        }
    const val DAYS = 50
    const val BANKING = 51
    const val PEOPLE = 52
    const val LOCATIONS = 53
    const val BANKINGSIDECAR = 54
    const val PROFILEPICTURE = 55
    const val COMMITS = 56
    const val MANUALTRANSACTION = 57
    const val TODOS = 58
    const val TRANSACTIONSPLIT = 59
    const val EXTENSION = 60
    val EVENTRANGE = 1..10
}
