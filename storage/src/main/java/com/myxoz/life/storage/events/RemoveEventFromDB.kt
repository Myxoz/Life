package com.myxoz.life.storage.events

import com.myxoz.life.storage.dbwrapper.events.WriteEventDetailsDao

internal object RemoveEventFromDB {
    suspend fun removeSpontFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeSpont(id)
        eventDetailsDao.removeTags(id)
    }
    suspend fun removeHobbyFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeTags(id)
        eventDetailsDao.removeHobby(id)
    }
    suspend fun removeWorkFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeTags(id)
        eventDetailsDao.removeWork(id)
    }
    suspend fun removeLearnFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeTags(id)
        eventDetailsDao.removeLearn(id)
    }
    suspend fun removeSocialFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeTags(id)
        eventDetailsDao.removeSocial(id)
        eventDetailsDao.deletePeopleMapping(id)
    }
    suspend fun removeDigSocFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeDigSoc(id)
        eventDetailsDao.deleteDigSocMapping(id)
        eventDetailsDao.deletePeopleMapping(id)
    }
    suspend fun removeTimewasteFromDB(eventDetailsDao: WriteEventDetailsDao, id: Long) {
        eventDetailsDao.removeTimewaste(id)
        eventDetailsDao.removeAllTimewastePlatformsFor(id)
    }
}