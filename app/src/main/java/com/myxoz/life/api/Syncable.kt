package com.myxoz.life.api

import android.content.Context
import android.util.Log
import com.myxoz.life.api.syncables.BankingSidecarSyncable
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.syncables.ManualTransactionSyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import org.json.JSONObject

interface ServerSyncableCompanion<T: ServerSyncable> {
    /** Overwrite the content in the db. This should be called after the repo was updated for it to react to cache changes e.x. Event moved from day a to b */
    suspend fun overwriteDBByJson(db: API.WriteSyncableDaos, json: JSONObject): T =
        fromJSON(json).apply { saveToDB(db) }
    fun fromJSON(json: JSONObject): T
}

abstract class ServerSyncable(val calendarId: Int) {
    fun addTypeToJson(json: JSONObject): JSONObject = json.put("type", calendarId)
    abstract suspend fun saveToDB(db: API.WriteSyncableDaos)
}

abstract class Syncable(
    calendarId: Int,
    open val id: Long
) : ServerSyncable(calendarId) {
    suspend fun toJson(): JSONObject? =
        specificsToJson()?.let { addTypeToJson(it).put("id", id.toString()) }

    abstract suspend fun specificsToJson(): JSONObject?
    suspend fun addToWaitingSyncDao(db: WaitingSyncDao) {
        db.insertWaitingSync(
            WaitingSyncEntity(
                id,
                calendarId,
                System.currentTimeMillis()
            )
        )
    }

    suspend fun updateAndStageSync(db: API.WriteSyncableDaos, waitingSync: WaitingSyncDao) {
        saveToDB(db)
        addToWaitingSyncDao(waitingSync)
    }

    open fun getInvalidReason(): String? = null

    companion object {
        suspend fun from(entry: WaitingSyncEntity, readSyncableDaos: API.ReadSyncableDaos, context: Context): Syncable? {
            return when (entry.calendar) {
                in -Int.MIN_VALUE..<0 -> {
                    DeleteEntry(entry.calendar, entry.id)
                }

                SpecialSyncablesIds.DAYS -> {
                    val daysEntry = readSyncableDaos.daysDao.getDay(entry.id)
                    if (daysEntry == null) {
                        Log.w("API", "FullDayEvent should be synced but is not present in db")
                        return null
                    }
                    FullDaySyncable(
                        daysEntry.happyness,
                        daysEntry.stress,
                        daysEntry.successfulness,
                        daysEntry.steps,
                        daysEntry.screenTimeMs,
                        readSyncableDaos.daysDao.getScreenTimesByDay(entry.id),
                        entry.id,
                    )
                }
                SpecialSyncablesIds.MANUALTRANSACTION -> {
                    val entity = readSyncableDaos.bankingDao.getManualTransaction(entry.id) ?:return null
                    ManualTransactionSyncable.fromEntity(entity)
                }

                SpecialSyncablesIds.PEOPLE -> {
                    val dbEntry = readSyncableDaos.peopleDao.getPersonById(entry.id) ?: return null
                    PersonSyncable.from(readSyncableDaos.peopleDao, dbEntry)
                }

                SpecialSyncablesIds.LOCATIONS -> {
                    val dbEntry = readSyncableDaos.locationsDao.getLocationById(entry.id) ?: return null
                    LocationSyncable.from(dbEntry)
                }

                SpecialSyncablesIds.BANKINGSIDECAR -> {
                    val dbEntry = readSyncableDaos.bankingDao.getSidecar(entry.id) ?: return null
                    BankingSidecarSyncable(
                        dbEntry.id,
                        dbEntry.transactionId,
                        dbEntry.name,
                        dbEntry.date
                    )
                }

                SpecialSyncablesIds.PROFILEPICTURE -> {
                    ProfilePictureSyncable.fromDB(entry.id, context)
                }

                else -> {
                    val dbEntry =  readSyncableDaos.eventDetailsDao.getEvent(entry.id)
                    if (dbEntry == null) {
                        Log.w(
                            "API",
                            "Tried to create sync entry from non existing event in events table: Cal ${entry.calendar}: ${entry.id}"
                        )
                        return null
                    }
                    val dbEvent = SyncedEvent.from(readSyncableDaos.eventDetailsDao, dbEntry)
                    if (dbEvent == null) {
                        Log.w(
                            "API",
                            "Tried to construct syncedEvent from unknown calendar (SyncedEvent.from -> ProposedEvent.from)"
                        )
                        return null
                    }
                    dbEvent
                }
            }
        }
    }

    object SpecialSyncablesIds {
        const val DAYS = 50
        const val BANKING = 51
        const val PEOPLE = 52
        const val LOCATIONS = 53
        const val BANKINGSIDECAR = 54
        const val PROFILEPICTURE = 55
        const val COMMITS = 56
        const val MANUALTRANSACTION = 57
    }
    interface DatedSyncable{
        val timestamp: Long
        val id: Long
        val calendarId: Int
        fun copyWithTs(timestamp: Long): DatedSyncable
        suspend fun toJson(): JSONObject?
        suspend fun specificsToJson(): JSONObject?
        suspend fun addToWaitingSyncDao(db: WaitingSyncDao)
        suspend fun updateAndStageSync(db: API.WriteSyncableDaos, waitingSync: WaitingSyncDao)
        fun getInvalidReason(): String? = null
    }
}