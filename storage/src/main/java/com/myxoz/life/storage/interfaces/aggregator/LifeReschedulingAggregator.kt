package com.myxoz.life.storage.interfaces.aggregator

import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.convertion.events.EventsFromDatabase
import com.myxoz.life.storage.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.LocationInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import java.time.Instant

class LifeReschedulingAggregator internal constructor(
    private val keyValueStore: KeyValueStore,
    private val calendarInterface: CalendarInterface,
    private val readEventDetailsDao: ReadEventDetailsDao,
    private val locationInterface: LocationInterface,
    private val peopleInterface: PeopleInterface,
) {
    private val store = KeyValueStorePrefix("liferescheduling")
    private val NEXTSCHEDULINGTIMESTAMPKEY = store.keyOfType("next_scheduled_ts", KeyValueType.InstantType)
    private val OBSERVEDEVENTIDKEY = store.keyOfType("observed_event_id", KeyValueType.StringListType)
    var nextScheduledTimestamp by keyValueStore.getAsState(NEXTSCHEDULINGTIMESTAMPKEY)
    var observedEventIds by keyValueStore.getAsState(OBSERVEDEVENTIDKEY)
    suspend fun getEventAt(now: Instant): List<SyncedEvent> {
        val ms = now.toEpochMilli()
        val event = readEventDetailsDao.getEventsOverlapping(ms, ms)
        return EventsFromDatabase.getBatchedEvents(event, readEventDetailsDao)
    }
    suspend fun getNextEvent(now: Instant): SyncedEvent? {
        val nextEvent = readEventDetailsDao.getEventAfter(now.toEpochMilli())
        return EventsFromDatabase.byEntity(nextEvent, readEventDetailsDao)
    }
    fun getLocationById(id: Long) = locationInterface.getLocationById(id)
    fun getPeople(id: Set<Long>) = peopleInterface.getPeople(id)
    fun subscribeAsHook(hook: CalendarInterface.CalendarHook) {
        calendarInterface.addHook(hook)
    }
}