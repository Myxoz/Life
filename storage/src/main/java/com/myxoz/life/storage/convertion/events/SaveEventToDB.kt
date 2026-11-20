package com.myxoz.life.storage.convertion.events

import com.myxoz.life.storage.dbwrapper.events.DigSocEntity
import com.myxoz.life.storage.dbwrapper.events.DigSocMappingEntity
import com.myxoz.life.storage.dbwrapper.events.HobbyEntiy
import com.myxoz.life.storage.dbwrapper.events.LearnEntity
import com.myxoz.life.storage.dbwrapper.events.PeopleMappingEntity
import com.myxoz.life.storage.dbwrapper.events.SocialEntity
import com.myxoz.life.storage.dbwrapper.events.SpontEntity
import com.myxoz.life.storage.dbwrapper.events.TagsEntity
import com.myxoz.life.storage.dbwrapper.events.TimewasteEntity
import com.myxoz.life.storage.dbwrapper.events.TimewastePlatformEntity
import com.myxoz.life.storage.dbwrapper.events.TravelEntity
import com.myxoz.life.storage.dbwrapper.events.VehicleEntity
import com.myxoz.life.storage.dbwrapper.events.WorkEntity
import com.myxoz.life.storage.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TagEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent

internal object SaveEventToDB {
    suspend fun RawEvent.saveToDB(eventId: Long, writeEventDao: WriteEventDetailsDao) {
        if(this is TagEvent) this.storeTags(eventId, writeEventDao)
        if(this is PeopleEvent) this.savePeopleMapping(eventId, writeEventDao)
        when(this) {
            is EmptyEvent -> Unit
            is SleepEvent -> Unit
            is SpontEvent -> writeEventDao.insertSpont(SpontEntity(eventId, title))
            is SocialEvent -> writeEventDao.insertSocial(SocialEntity(eventId, title, more))
            is HobbyEvent -> writeEventDao.insertHobby(HobbyEntiy(eventId, title, details))
            is TravelEvent -> save(eventId, writeEventDao)
            is LearnEvent -> writeEventDao.insertLearn(LearnEntity(eventId, title, details))
            is TimewasteEvent -> save(eventId, writeEventDao)
            is DigSocEvent -> save(eventId, writeEventDao)
            is WorkEvent -> writeEventDao.insertWork(WorkEntity(eventId, title, details))
        }
    }

    private suspend fun TagEvent.storeTags(eventId: Long, writeEventDetailsDao: WriteEventDetailsDao) {
        writeEventDetailsDao.insertTags(
            eventTags.map {
                TagsEntity(eventId, it.id)
            }
        )
    }
    private suspend fun PeopleEvent.savePeopleMapping(eventId: Long, db: WriteEventDetailsDao) {
        db.insertAllPeopleMappings(
            people.map {
                PeopleMappingEntity(eventId, it)
            }
        )
    }

    private suspend fun DigSocEvent.save(eventId: Long, db: WriteEventDetailsDao) {
        db.insertDicSoc(DigSocEntity(eventId, title))
        db.insertAllDigSocMappings(
            digSocEntries.map {
                DigSocMappingEntity(eventId, it.type.id, it.duration.inWholeMilliseconds)
            }
        )
    }

    private suspend fun TimewasteEvent.save(eventId: Long, db: WriteEventDetailsDao) {
        db.insertTimewaste(TimewasteEntity(eventId, title))
        db.insertTimewastePlatforms(
            timewastePlatforms.map {
                TimewastePlatformEntity(eventId, it.type.id, it.duration.inWholeMilliseconds)
            }
        )
    }

    private suspend fun TravelEvent.save(eventId: Long, db: WriteEventDetailsDao) {
        db.insertTravel(TravelEntity(eventId, from, to))
        vehicles.forEach {
            db.insertVehicle(
                VehicleEntity(eventId, it.type.id, it.duration.inWholeMilliseconds)
            )
        }
    }
}