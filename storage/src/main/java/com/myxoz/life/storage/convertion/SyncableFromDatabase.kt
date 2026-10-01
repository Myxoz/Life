package com.myxoz.life.storage.convertion

import android.content.Context
import android.util.Log
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.API.Companion.LOGTAG
import com.myxoz.life.storage.api.syncables.DeleteEntry
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.api.syncables.Syncable
import com.myxoz.life.storage.api.syncables.SyncableIds
import com.myxoz.life.storage.api.syncables.SyncableIds.EVENTRANGE
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.convertion.events.EventsFromDatabase
import com.myxoz.life.storage.dbwrapper.WaitingSyncEntity
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.storage.dbwrapper.days.ReadDaysDao
import com.myxoz.life.storage.dbwrapper.extension.ReadExtensionDao
import com.myxoz.life.storage.dbwrapper.locations.ReadLocationsDao
import com.myxoz.life.storage.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.storage.dbwrapper.todos.ReadTodosDao

internal object SyncableFromDatabase {
    suspend fun fromWaitingSync(entities: List<WaitingSyncEntity>, readSyncableDaos: API.ReadSyncableDaos, context: Context): List<Syncable> =
        from(entities.map { it.calendar to it.id }, readSyncableDaos, context)
    suspend fun from(entities: List<Pair<Int, Long>>, readSyncableDaos: API.ReadSyncableDaos, context: Context): List<Syncable> {
        val eventIds = entities.mapNotNull { if(it.first in EVENTRANGE) it.second else null }
        val events = EventsFromDatabase.getEventsFromDatabse(eventIds, readSyncableDaos.eventDetailsDao)
        val syncables = entities.mapNotNull { entry ->
            fromSyncable(entry.first, entry.second, readSyncableDaos, context)
        }
        return events + syncables
    }
    suspend fun fromSyncable(calendarId: Int, id: Long, readSyncableDaos: API.ReadSyncableDaos, context: Context): Syncable? {
        val syncable: Syncable? = when (calendarId) {
            in -Int.MIN_VALUE..<0 -> getDeleteEntry(calendarId, id)
            SyncableIds.DAYS -> getFullDaySyncable(id, readSyncableDaos.daysDao)
            SyncableIds.PEOPLE -> getPersonSyncable(id, readSyncableDaos.peopleDao)
            SyncableIds.LOCATIONS -> getLocation(id, readSyncableDaos.locationsDao)
            SyncableIds.BANKINGSIDECAR -> getBankingSidecar(id, readSyncableDaos.bankingDao)
            SyncableIds.PROFILEPICTURE -> getProfilePicture(id, readSyncableDaos.peopleDao, context)
            SyncableIds.MANUALTRANSACTION -> getManualTransaction(id, readSyncableDaos.bankingDao)
            SyncableIds.TODOS -> getTodo(id, readSyncableDaos.todosDao)
            SyncableIds.TRANSACTIONSPLIT -> getSplitTransaction(id, readSyncableDaos.bankingDao)
            SyncableIds.EXTENSION -> getExtensionSyncable(id, readSyncableDaos.extensionDao)
            in EVENTRANGE -> EventsFromDatabase.getEventFromDatabaseUnbatched(id, readSyncableDaos.eventDetailsDao)
            else -> error("Tried to decode unknown event type $id in $calendarId")
        }
        if(syncable == null) Log.d(LOGTAG, "Syncable could not be reconstructed. Look into it: $id in $calendarId")
        return syncable
    }
    private fun getDeleteEntry(calendar: Int, id: Long) = DeleteEntry(-calendar, id)
    suspend fun getFullDaySyncable(id: Long, daysDao: ReadDaysDao): FullDaySyncable? =
        daysDao.getDay(id)?.fromEntity(daysDao)

    private suspend fun getManualTransaction(id: Long, bankingDao: ReadBankingDao) =
        bankingDao.getManualTransaction(id)?.fromEntity()

    private suspend fun getPersonSyncable(id: Long, personDao: ReadPeopleDao) =
        personDao.getPersonById(id)?.fromEntity(personDao)

    private suspend fun getLocation(id: Long, locationDao: ReadLocationsDao) =
        locationDao.getLocationById(id)?.fromEntity()

    private suspend fun getBankingSidecar(id: Long, bankingDao: ReadBankingDao) =
        bankingDao.getSidecar(id)?.fromEntity()

    private suspend fun getProfilePicture(id: Long, personDao: ReadPeopleDao, context: Context) =
        personDao.getPPById(id)?.fromEntity(context)

    private suspend fun getTodo(id: Long, todoDao: ReadTodosDao) =
        todoDao.getById(id)?.fromEntity()

    private suspend fun getSplitTransaction(id: Long, db: ReadBankingDao) =
        db.getTransactionSplit(id)?.fromEntity(db)

    private suspend fun getExtensionSyncable(id: Long, extensionDao: ReadExtensionDao): ExtensionSyncable? {
        val dbEntry = extensionDao.getById(id) ?: return null
        val type = ExtensionSyncable.ExtensionSyncableType.typeFromId(id) ?: return null
        return ExtensionSyncable(type, type.parser.fromString(dbEntry.data))
    }
}