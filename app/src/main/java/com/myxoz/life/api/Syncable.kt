package com.myxoz.life.api

import android.util.Log
import com.myxoz.life.api.syncables.BankingSidecar
import com.myxoz.life.api.syncables.BankingSyncable
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.screens.feed.fullscreenevent.getEventId
import org.json.JSONObject

interface ServerSyncableCompanion {
    suspend fun overwriteByJson(db: StorageManager, it: JSONObject)
}

abstract class ServerSyncable(val calendarId: Int) {
    fun addTypeToJson(json: JSONObject): JSONObject = json.put("type", calendarId)

    companion object : ServerSyncableCompanion {
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            val cal = it.getInt("type")
            when (cal) {
                in Int.MIN_VALUE..<0 -> {
                    val id = it.getEventId()
                    db.events.removeById(id)
                    when (it.getInt("type")) { // GOTO AEFL
                        EventType.Sleep.id -> {}
                        EventType.Spont.id -> {
                            db.tags.removeById(id)
                            db.spont.removeById(id)
                        }

                        EventType.Hobby.id -> {
                            db.tags.removeById(id)
                            db.hobby.removeById(id)
                        }

                        EventType.Work.id -> {
                            db.tags.removeById(id)
                            db.work.removeById(id)
                        }

                        EventType.Learn.id -> {
                            db.tags.removeById(id)
                            db.learn.removeById(id)
                        }

                        EventType.Social.id -> {
                            db.tags.removeById(id)
                            db.social.removeById(id)
                            db.peopleMapping.deleteMappingByEventId(id)
                        }

                        EventType.DigSoc.id -> {
                            db.digsoc.removeById(id)
                            db.digsocMapping.deleteMappingByEventId(id)
                            db.peopleMapping.deleteMappingByEventId(id)
                        }
                    }
                }

                Syncable.SpecialSyncablesIds.BANKING -> {
                    BankingSyncable.overwriteByJson(db, it)
                }

                Syncable.SpecialSyncablesIds.DAYS -> {
                    FullDaySyncable.overwriteByJson(db, it)
                }

                Syncable.SpecialSyncablesIds.PEOPLE -> {
                    PersonSyncable.overwriteByJson(db, it)
                }

                Syncable.SpecialSyncablesIds.LOCATIONS -> {
                    Location.overwriteByJson(db, it)
                }

                Syncable.SpecialSyncablesIds.BANKINGSIDECAR -> {
                    BankingSidecar.overwriteByJson(db, it)
                }

                Syncable.SpecialSyncablesIds.PROFILEPICTURE -> {
                    ProfilePictureSyncable.overwriteByJson(db, it)
                }
                Syncable.SpecialSyncablesIds.COMMITS -> {
                    CommitSyncable.overwriteByJson(db, it)
                }

                else -> {
                    SyncedEvent.overwriteByJson(db, it)
                }
            }
        }
    }
}

abstract class Syncable(
    calendarId: Int,
    val id: Long
) : ServerSyncable(calendarId) {
    suspend fun toJson(db: StorageManager): JSONObject? =
        specificsToJson(db)?.let { addTypeToJson(it).put("id", id.toString()) }

    abstract suspend fun specificsToJson(db: StorageManager): JSONObject?
    suspend fun addToWaitingSyncDao(db: StorageManager) {
        db.waitingSync.insertWaitingSync(
            WaitingSyncEntity(
                id,
                calendarId,
                System.currentTimeMillis()
            )
        )
    }

    suspend fun saveAndSync(db: StorageManager) {
        saveToDB(db)
        addToWaitingSyncDao(db)
    }

    abstract suspend fun saveToDB(db: StorageManager)

    companion object {
        suspend fun from(entry: WaitingSyncEntity, db: StorageManager): Syncable? {
            return when (entry.calendar) {
                in -Int.MIN_VALUE..<0 -> {
                    DeleteEntry(entry.calendar, entry.id)
                }

                SpecialSyncablesIds.DAYS -> {
                    val daysEntry = db.days.getDay(entry.id.toInt())
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
                        entry.id
                    )
                }

                SpecialSyncablesIds.PEOPLE -> {
                    val dbEntry = db.people.getPersonById(entry.id) ?: return null
                    PersonSyncable.from(db, dbEntry)
                }

                SpecialSyncablesIds.LOCATIONS -> {
                    val dbEntry = db.location.getLocationById(entry.id) ?: return null
                    Location.from(dbEntry)
                }

                SpecialSyncablesIds.BANKINGSIDECAR -> {
                    val dbEntry = db.bankingSidecar.getSidecar(entry.id) ?: return null
                    BankingSidecar(
                        dbEntry.id,
                        dbEntry.transactionId,
                        dbEntry.name,
                        dbEntry.date
                    )
                }

                SpecialSyncablesIds.PROFILEPICTURE -> {
                    ProfilePictureSyncable(
                        entry.id
                    )
                }

                else -> {
                    val dbEntry = db.events.getEvent(entry.id)
                    if (dbEntry == null) {
                        Log.w(
                            "API",
                            "Tried to create sync entry from non existing event in events table: Cal ${entry.calendar}: ${entry.id}"
                        )
                        return null
                    }
                    val dbEvent = SyncedEvent.from(db, dbEntry)
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
    }
}