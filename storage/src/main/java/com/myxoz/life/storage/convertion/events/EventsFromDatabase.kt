package com.myxoz.life.storage.convertion.events

import com.myxoz.life.core.toInstant
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.dbwrapper.events.EventEntity
import com.myxoz.life.storage.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent
import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.TimewastePlatform
import com.myxoz.life.storage.events.additionals.Vehicle
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

internal object EventsFromDatabase {
    suspend fun getEventFromDatabaseUnbatched(id: Long, eventDetailsDao: ReadEventDetailsDao): SyncedEvent?{
        val eventEntity = eventDetailsDao.getEvent(id) ?: return null
        return getBatchedEvents(listOf(eventEntity), eventDetailsDao).firstOrNull()
    }
    suspend fun getEventsFromDatabse(ids: List<Long>, eventDetailsDao: ReadEventDetailsDao) =
        getBatchedEvents(eventDetailsDao.getEventsById(ids), eventDetailsDao)
    suspend fun byEntity(entity: EventEntity?, db: ReadEventDetailsDao): SyncedEvent? = getBatchedEvents(listOf(entity ?: return null), db).first()
    suspend fun getBatchedEvents(events: List<EventEntity>, db: ReadEventDetailsDao): List<SyncedEvent> {
        if(events.isEmpty()) return emptyList()
        val ids = events.map { it.id }
        val allTypes = events.map { it.type }.toSet()
        val containsTagEvent = allTypes.any { allTagEvents.contains(it) }
        val containsSocialEvent = allTypes.any { allSocialEvents.contains(it) }
        val tags = if(containsTagEvent)
            db.getAllTagsFor(ids).groupBy { it.id }.mapValues { event ->
                event.value.mapNotNull { EventTag.getTagById(it.tag) }.toSet()
            }
        else
            emptyMap()
        val spont = if(SpontEvent.CALENDARID in allTypes) db.getAllSpontFor(ids).associateBy { it.id } else emptyMap()
        val hobby = if(HobbyEvent.CALENDARID in allTypes) db.getAllHobbyFor(ids).associateBy { it.id } else emptyMap()
        val learn = if(LearnEvent.CALENDARID in allTypes) db.getAllLearnFor(ids).associateBy { it.id } else emptyMap()
        val social = if(SocialEvent.CALENDARID in allTypes) db.getAllSocialFor(ids).associateBy { it.id } else emptyMap()
        val travel = if(TravelEvent.CALENDARID in allTypes) db.getAllTravelFor(ids).associateBy { it.id } else emptyMap()
        val digSoc = if(DigSocEvent.CALENDARID in allTypes) db.getAllDigSocFor(ids).associateBy { it.id } else emptyMap()
        val work = if(WorkEvent.CALENDARID in allTypes) db.getAllWorkFor(ids).associateBy { it.id } else emptyMap()
        val timewaste = if(TimewasteEvent.CALENDARID in allTypes) db.getAllTimewasteFor(ids).associateBy { it.id } else emptyMap()
        val people = if(containsSocialEvent) db.getAllPeopleFor(ids).groupBy { it.eventId }.mapValues { event -> event.value.map { it.personId }.toSet()} else emptyMap()
        val vehicle = if(TravelEvent.CALENDARID in allTypes) db.getAllVehiclesFor(ids).groupBy { it.id }.mapValues {
            it.value.mapNotNull {TimedTagLikeContainer(Vehicle.getById(it.type) ?: return@mapNotNull null, it.durationMs.milliseconds)}
        } else emptyMap()
        val digSocMapping = if(DigSocEvent.CALENDARID in allTypes) db.getAllDigSocMappingFor(ids).groupBy { it.eventId }.mapValues {
            it.value.mapNotNull {TimedTagLikeContainer(DigSocPlatform.getById(it.app) ?: return@mapNotNull null, it.durationMs.milliseconds)}
        } else emptyMap()
        val twPlatform = if(TimewasteEvent.CALENDARID in allTypes) db.getAllTimewastePlatformsFor(ids).groupBy { it.eventId }.mapValues {
            it.value.mapNotNull {TimedTagLikeContainer(TimewastePlatform.getById(it.timewastePlatform) ?: return@mapNotNull null, it.durationMs.milliseconds)}
        }
        else emptyMap()
        return events.map {
            val eventTime = EventTime(
                Instant.ofEpochMilli(it.start),
                Instant.ofEpochMilli(it.end),
                it.uss,
                it.usl
            )
            val rawEvent = when(it.type) {
                EmptyEvent.CALENDARID -> EmptyEvent(eventTime)
                SleepEvent.CALENDARID -> SleepEvent(eventTime)
                SpontEvent.CALENDARID -> {
                    val event = spont[it.id]
                        ?: error("Tried to get spont event with no fields present in db")
                    val tags = tags[it.id] ?: setOf()
                    SpontEvent(eventTime, tags, event.title)
                }
                HobbyEvent.CALENDARID -> {
                    val event = hobby[it.id]
                        ?: error("Tried to get hobby event with no fields present in db")
                    val tags = tags[it.id] ?: setOf()
                    HobbyEvent(eventTime, tags, event.title, event.details)
                }
                LearnEvent.CALENDARID -> {
                    val event = learn[it.id]
                        ?: error("Tried to get learn event with no fields present in db")
                    val tags = tags[it.id] ?: setOf()
                    LearnEvent(eventTime, tags, event.title, event.details)
                }
                SocialEvent.CALENDARID -> {
                    val event = social[it.id]
                        ?: error("Tried to get social event with no fields present in db")
                    val tags = tags[it.id] ?: setOf()
                    val people = people[it.id] ?: setOf()
                    SocialEvent(eventTime, tags, event.title, people, event.more)
                }
                TravelEvent.CALENDARID -> {
                    val event = travel[it.id]
                        ?: error("Tried to get travel event with no fields present in db")
                    val vehicles = vehicle[it.id] ?: emptyList()
                    TravelEvent(eventTime, event.from, event.to, vehicles)
                }
                DigSocEvent.CALENDARID -> {
                    val event = digSoc[it.id]
                        ?: error("Tried to get digsoc event with no fields present in db")
                    val digsocMapping = digSocMapping[it.id] ?: emptyList()
                    val people = people[it.id] ?: setOf()
                    DigSocEvent(eventTime, digsocMapping, event.title, people)
                }
                WorkEvent.CALENDARID -> {
                    val event = work[it.id]
                        ?: error("Tried to get work event with no fields present in db")
                    val tags = tags[it.id] ?: setOf()
                    WorkEvent(eventTime, tags, event.title, event.details)
                }
                TimewasteEvent.CALENDARID -> {
                    val event = timewaste[it.id]
                        ?: error("Tried to get digsoc event with no fields present in db")
                    val timewaste = twPlatform[it.id] ?: emptyList()
                    TimewasteEvent(eventTime, timewaste, event.title ?: "")
                }
                else -> error("Cannot reconstruct event from database")
            }
            SyncedEvent(it.id, it.created.toInstant(), it.edited?.toInstant(), rawEvent)
        }
    }
    val allTagEvents = setOf(SpontEvent.CALENDARID, HobbyEvent.CALENDARID, LearnEvent.CALENDARID,
        SocialEvent.CALENDARID, WorkEvent.CALENDARID)
    val allSocialEvents = setOf(SocialEvent.CALENDARID, DigSocEvent.CALENDARID)
}