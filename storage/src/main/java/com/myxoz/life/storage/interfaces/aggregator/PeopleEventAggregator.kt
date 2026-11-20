package com.myxoz.life.storage.interfaces.aggregator

import com.myxoz.life.core.minus
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.convertion.events.EventsFromDatabase
import com.myxoz.life.storage.dbwrapper.events.EventEntity
import com.myxoz.life.storage.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.storage.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.storage.interfaces.CalendarInterface
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import java.time.Instant
import kotlin.time.Duration

class PeopleEventAggregator internal constructor(
    private val calendarInterface: CalendarInterface,
    private val readPeopleDao: ReadPeopleDao,
    private val readEventsDao: ReadEventDetailsDao
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNextInteraction(personId: Long) = calendarInterface.interactedWithPerson(personId).mapLatest {
        getByEventEntity(
            readPeopleDao.getNextInteractionByPerson(personId, System.currentTimeMillis())
        )
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLastInteraction(personId: Long) = calendarInterface.interactedWithPerson(personId).mapLatest {
        getByEventEntity(
            readPeopleDao.getLastInteractionByPerson(personId, System.currentTimeMillis())
        )
    }
    private suspend fun getByEventEntity(entity: EventEntity?): SyncedEvent? =
        EventsFromDatabase.byEntity(entity, readEventsDao)

    suspend fun getSocialEventsTimesInBetweenMapped(start: Long, end: Long) =
        readEventsDao.getAllPeopleEvents(start, end).associateBy { it.id }.mapValues {
            it.value.start to it.value.end
        }
    suspend fun getEventsWithPerson(id: Long, timeframe: Duration?): Pair<List<SyncedEvent>, List<SyncedEvent>> {
        val rawEvents = readEventsDao.getEventsWithPerson(
            id,
            timeframe?.let { Instant.now() - it }?.toEpochMilli() ?: 0L,
            System.currentTimeMillis()
        )
        val events = EventsFromDatabase.getBatchedEvents(rawEvents, readEventsDao)
        val rawOverlappingEvents = readEventsDao.getEventsById(rawEvents.map { it.id })
        val overlappingEvents = EventsFromDatabase.getBatchedEvents(rawOverlappingEvents, readEventsDao)
        return events to overlappingEvents
    }
}