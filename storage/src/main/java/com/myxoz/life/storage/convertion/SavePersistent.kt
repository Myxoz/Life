package com.myxoz.life.storage.convertion

import android.content.Context
import android.util.Log
import com.myxoz.life.storage.AutoDetectLocalEvent
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
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable.Companion.saveBase64ToFile
import com.myxoz.life.storage.api.syncables.ServerSyncable
import com.myxoz.life.storage.api.syncables.Syncable
import com.myxoz.life.storage.api.syncables.SyncableIds.getCalendarId
import com.myxoz.life.storage.api.syncables.SyncableIds.getEventCalendarId
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.convertion.SyncableToJSON.toJSON
import com.myxoz.life.storage.convertion.events.EventsToJSON.eventToJSONWithType
import com.myxoz.life.storage.convertion.events.SaveEventToDB.saveToDB
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao
import com.myxoz.life.storage.dbwrapper.WaitingSyncEntity
import com.myxoz.life.storage.dbwrapper.banking.BankingEntity
import com.myxoz.life.storage.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.storage.dbwrapper.banking.ManualTransactionEntity
import com.myxoz.life.storage.dbwrapper.banking.TransactionSplitEntity
import com.myxoz.life.storage.dbwrapper.banking.TransactionSplitPartEntity
import com.myxoz.life.storage.dbwrapper.banking.WriteBankingDao
import com.myxoz.life.storage.dbwrapper.commits.CommitEntity
import com.myxoz.life.storage.dbwrapper.commits.WriteCommitsDao
import com.myxoz.life.storage.dbwrapper.days.DayScreenTimeEntity
import com.myxoz.life.storage.dbwrapper.days.DaysEntity
import com.myxoz.life.storage.dbwrapper.days.WriteDaysDao
import com.myxoz.life.storage.dbwrapper.extension.ExtensionEntity
import com.myxoz.life.storage.dbwrapper.extension.WriteExtensionDao
import com.myxoz.life.storage.dbwrapper.localevent.AutoDetectLocalEventEntity
import com.myxoz.life.storage.dbwrapper.localevent.AutoDetectLocalEventsDao
import com.myxoz.life.storage.dbwrapper.locations.LocationEntity
import com.myxoz.life.storage.dbwrapper.locations.WriteLocationsDao
import com.myxoz.life.storage.dbwrapper.people.PersonEntity
import com.myxoz.life.storage.dbwrapper.people.ProfilePictureStored
import com.myxoz.life.storage.dbwrapper.people.SocialsEntity
import com.myxoz.life.storage.dbwrapper.people.WritePeopleDao
import com.myxoz.life.storage.dbwrapper.todos.TodoEntity
import com.myxoz.life.storage.dbwrapper.todos.WriteTodosDao
import org.json.JSONArray

internal object SavePersistent{
    suspend fun saveToPersistentStorage(
        syncable: ServerSyncable,
        db: API.WriteSyncableDaos,
        readSyncableDaos: API.ReadSyncableDaos,
        context: Context
    ) {
        when (syncable) {
            is BankingSidecarSyncable -> syncable.saveToDB(db.bankingDao)
            is DeleteEntry -> syncable.applyToDB(db, readSyncableDaos, context)
            is ExtensionSyncable -> syncable.saveToDB(db.extensionDao)
            is FullDaySyncable -> syncable.saveToDB(db.daysDao)
            is LocationSyncable -> syncable.saveToDB(db.locationsDao)
            is ManualTransactionSyncable -> syncable.saveToDB(db.bankingDao)
            is PersonSyncable -> syncable.saveToDB(db.peopleDao)
            is ProfilePictureSyncable -> syncable.saveToDB(context, db.peopleDao)
            is SyncedEvent -> syncable.raw.saveToDB(syncable.id, db.eventDetailsDao)
            is TodoSyncable -> syncable.saveToDB(db.todosDao)
            is TransactionSplitSyncable -> syncable.saveToDB(db.bankingDao)
            is BankingSyncable -> syncable.saveToDB(db.bankingDao)
            is CommitSyncable -> syncable.saveToDB(db.commitsDao)
        }
    }

    suspend fun DeleteEntry.applyToDB(writeSyncableDaos: API.WriteSyncableDaos, readSyncableDaos: API.ReadSyncableDaos, context: Context) {
        // This might be fascinating infrastructure: Saving a DeleteEntry into the database
        // Just calls removeSyncable on the databse. Take the perspective of "applying" a
        // patch to the databse, while most of them just apply "add" or "modify", deletion is an option
        val oldSyncable = SyncableFromDatabase.fromSyncable(
            deleteCalendarId, id, readSyncableDaos, context
        ) ?: return
        RemoveSyncableFromDatabase.removeSyncable(oldSyncable, writeSyncableDaos, context, true)
    }

    suspend fun ManualTransactionSyncable.saveToDB(writeBankingDao: WriteBankingDao) {
        writeBankingDao.insertManualTransaction(
            ManualTransactionEntity(id, digital, cashless, amountCents, name, timestamp.toEpochMilli(), purpose)
        )
    }
    suspend fun FullDaySyncable.saveToDB(daysDao: WriteDaysDao) {
        daysDao.insertDay(DaysEntity(id, screenTimeMs.inWholeMilliseconds.toInt(), steps, happyness, stress, successfulness))
        daysDao.insertAllDayScreenTime(topApps.map {
            DayScreenTimeEntity(
                id,
                it.packagename,
                it.duration.inWholeMilliseconds
            )
        })
    }
    suspend fun BankingSyncable.saveToDB(writeBankinngDao: WriteBankingDao) {
        writeBankinngDao.insertTransaction(
            BankingEntity(id, card, transfer, amountCents, currency, bookingTime, valueDate.toEpochMilli(),
                fromName, fromIban, fromBic, purpose, saldoAfterCents, purposeDate?.toEpochMilli(), lastUpdate.toEpochMilli())
        )
    }

    suspend fun TransactionSplitSyncable.saveToDB(writeSplitSyncable: WriteBankingDao) {
        writeSplitSyncable.insertSplitAndParts(
            TransactionSplitEntity(id,syncableId,remoteId),
            parts.map { TransactionSplitPartEntity(id, it.person, it.amount) }
        )
    }


    suspend fun TodoSyncable.saveToDB(todosDao: WriteTodosDao) {
        todosDao.insertTodo(TodoEntity(id, short, details, done, timestamp.toEpochMilli()))
    }

    suspend fun PersonSyncable.saveToDB(peopleDao: WritePeopleDao) {
        peopleDao.insertPerson(
            PersonEntity(id, name, fullName, phoneNumber, iban, home, birthday?.toEpochDay())
        )
        peopleDao.insertAllSocials(
            socials.map {
                SocialsEntity(id, it.platform.id, it.handle ?: "")
            }
        )
    }

    suspend fun LocationSyncable.saveToDB(locationDao: WriteLocationsDao) {
        locationDao.insertLocation(
            LocationEntity(id, name, longitude, lat, radiusM, ssid, street, number, city, country)
        )
    }

    suspend fun CommitSyncable.saveToDB(commitsDao: WriteCommitsDao) {
        commitsDao.insert(
            CommitEntity(
                repoOwner,
                repoName,
                commitSha,
                commitMessage,
                commitAuthor,
                commitEmail,
                commitDate?.toEpochMilli(),
                additions,
                deletions,
                filesChanged,
                JSONArray()
                    .apply {
                        modifications.forEach {
                            put(it.toJSON())
                        }
                    }
                    .toString(),
                commitUrl,
                updated.toEpochMilli(),
            )
        )
    }

    suspend fun ProfilePictureSyncable.saveToDB(context: Context, peopleDao: WritePeopleDao) {
        try {
            val hasPP = bitmapBase64 != null && saveBase64ToFile(context, bitmapBase64, id) != null
            peopleDao.insertProfilePicture(ProfilePictureStored(id, hasPP))
        } catch (e: Exception) {
            Log.e("ImagePicker","Failed to overwrite Bitmap from ServerResponse", e)
        }
    }
    suspend fun ExtensionSyncable.saveToDB(extensionDao: WriteExtensionDao){
        extensionDao.insertExtension(
            ExtensionEntity(
                type.id,
                content.specificsToString()
            )
        )
    }

    suspend fun BankingSidecarSyncable.saveToDB(bankingDao: WriteBankingDao) {
        bankingDao.insertSidecar(
            BankingSidecarEntity(id, transactionId, name, date.toEpochMilli())
        )
    }
    suspend fun Syncable.saveAndRequestSync(waitingSyncDao: WaitingSyncDao, writeSyncableDaos: API.WriteSyncableDaos, readSyncableDaos: API.ReadSyncableDaos, context: Context) {
        waitingSyncDao.insertWaitingSync(
            WaitingSyncEntity(id, getCalendarId(), System.currentTimeMillis())
        )
        API.requestSync()
        RemoveSyncableFromDatabase.removeSyncable(this, writeSyncableDaos, context, false)
        saveToPersistentStorage(this, writeSyncableDaos, readSyncableDaos, context)
    }
    suspend fun AutoDetectLocalEvent.saveToDB(autoDetectLocalEventsDao: AutoDetectLocalEventsDao) {
        autoDetectLocalEventsDao.insertAutoDetectLocalEvent(AutoDetectLocalEventEntity(
            localId,
            raw.getEventCalendarId(),
            raw.eventTime.start.toEpochMilli(),
            raw.eventTime.end.toEpochMilli(),
            raw.eventToJSONWithType().asString()
        ))
    }
}