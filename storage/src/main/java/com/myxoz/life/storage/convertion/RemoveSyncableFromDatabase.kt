package com.myxoz.life.storage.convertion

import android.content.Context
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.syncables.BankingSidecarSyncable
import com.myxoz.life.storage.api.syncables.BankingSyncable
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.api.syncables.DeleteEntry
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.api.syncables.ManualTransactionSyncable
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable.Companion.deleteBitmapFile
import com.myxoz.life.storage.api.syncables.ServerSyncable
import com.myxoz.life.storage.api.syncables.Syncable
import com.myxoz.life.storage.api.syncables.SyncableIds.getCalendarId
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao
import com.myxoz.life.storage.dbwrapper.WaitingSyncEntity
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
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.TagEvent

internal object RemoveSyncableFromDatabase {
    /**
     *  throroughDeletion seams werid. If this flag is set, we only remove data, that will not
     *  just be overriden while saving. This is needed for Entities with joined primary keys, that
     *  might not be overriden when just saving by id.
     */
    suspend fun removeSyncable(syncable: ServerSyncable, db: API.WriteSyncableDaos, context: Context, thoroughDeletion: Boolean) {
        when(syncable) {
            is BankingSidecarSyncable -> if(thoroughDeletion) error("Banking sidecars are not deletable")
            is DeleteEntry -> if(thoroughDeletion) error("Removing a DeleteEntry is impossible $syncable")
            is ExtensionSyncable -> if(thoroughDeletion) db.extensionDao.removeExtensionById(syncable.id)
            is FullDaySyncable -> if(thoroughDeletion) error("Cannot delete FullDaySyncable")
            is LocationSyncable -> if(thoroughDeletion) error("Locations are not deletable")
            is ManualTransactionSyncable -> if(thoroughDeletion) db.bankingDao.deleteManualTransactionSyncable(syncable.id)
            is PersonSyncable -> if(thoroughDeletion) error("People are not deletable")
            is ProfilePictureSyncable -> {
                if(thoroughDeletion) error("Profile Pictures are not deletable")
                deleteBitmapFile(context, syncable.id)
            }
            is TodoSyncable -> if(thoroughDeletion) db.todosDao.removeTodoById(syncable.id)
            is TransactionSplitSyncable -> {
                if(thoroughDeletion) db.bankingDao.deleteTransactionSplit(syncable.id)
                db.bankingDao.deleteTransactionSplitParts(syncable.id)
            }
            is BankingSyncable -> if(thoroughDeletion) error("Bankingentries are not deletable")
            is CommitSyncable -> if(thoroughDeletion) error("Commits are not deletable")
            is SyncedEvent -> {
                val eventDetailsDao = db.eventDetailsDao
                if(thoroughDeletion) eventDetailsDao.removeEventById(syncable.id)
                if(syncable.raw is PeopleEvent) eventDetailsDao.deletePeopleMapping(syncable.id)
                if(syncable.raw is TagEvent) eventDetailsDao.deletePeopleMapping(syncable.id)
                when(syncable.raw) {
                    is DigSocEvent -> {
                        if(thoroughDeletion) eventDetailsDao.removeDigSoc(syncable.id)
                        eventDetailsDao.deleteDigSocMapping(syncable.id)
                    }
                    is EmptyEvent -> error("Trying to remove empty event")
                    is HobbyEvent -> if(thoroughDeletion) eventDetailsDao.removeHobby(syncable.id)
                    is LearnEvent -> if(thoroughDeletion) eventDetailsDao.removeLearn(syncable.id)
                    is SleepEvent -> { /* only present in event table, already in removeEventById*/}
                    is SocialEvent -> if(thoroughDeletion) eventDetailsDao.removeSocial(syncable.id)
                    is SpontEvent -> if(thoroughDeletion) eventDetailsDao.removeSpont(syncable.id)
                    is TimewasteEvent -> {
                        if(thoroughDeletion) eventDetailsDao.removeTimewaste(syncable.id)
                        eventDetailsDao.removeAllTimewastePlatformsFor(syncable.id)
                    }
                    is TravelEvent -> {
                        if(thoroughDeletion) eventDetailsDao.removeTravel(syncable.id)
                        eventDetailsDao.removeVehicleById(syncable.id)
                    }
                    is WorkEvent -> if(thoroughDeletion) eventDetailsDao.removeWork(syncable.id)
                }
            }
        }
    }

    suspend fun Syncable.deleteAndDoNotSync(writeSyncableDaos: API.WriteSyncableDaos, context: Context, thoroughDeletion: Boolean) {
        removeSyncable(this, writeSyncableDaos, context, thoroughDeletion)
    }
    suspend fun Syncable.deleteAndSync(waitingSyncDao: WaitingSyncDao, writeSyncableDaos: API.WriteSyncableDaos, context: Context) {
        val calendarId = getCalendarId()
        waitingSyncDao.deleteWaitingSync(id, calendarId)
        waitingSyncDao.insertWaitingSync(
            WaitingSyncEntity(id, -calendarId, System.currentTimeMillis())
        )
        deleteAndDoNotSync(writeSyncableDaos, context, true)
    }
}