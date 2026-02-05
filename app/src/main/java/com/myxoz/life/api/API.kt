package com.myxoz.life.api

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.edit
import com.myxoz.life.api.syncables.BankingSidecarSyncable
import com.myxoz.life.api.syncables.BankingSyncable
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.Daos
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.dbwrapper.banking.WriteBankingDao
import com.myxoz.life.dbwrapper.commits.ReadCommitsDao
import com.myxoz.life.dbwrapper.commits.WriteCommitsDao
import com.myxoz.life.dbwrapper.days.ReadDaysDao
import com.myxoz.life.dbwrapper.days.WriteDaysDao
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.dbwrapper.locations.ReadLocationsDao
import com.myxoz.life.dbwrapper.locations.WriteLocationsDao
import com.myxoz.life.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.dbwrapper.people.WritePeopleDao
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.repositories.CalendarRepo
import com.myxoz.life.repositories.CommitsRepo
import com.myxoz.life.repositories.DaySummaryRepo
import com.myxoz.life.repositories.LocationRepo
import com.myxoz.life.repositories.PeopleRepo
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.forEach
import com.myxoz.life.utils.jsonObjArray
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

@Stable
class API(
    private val calendarRepo: CalendarRepo,
    private val daySummaryRepo: DaySummaryRepo,
    private val peopleRepo: PeopleRepo,
    private val bankingRepo: BankingRepo,
    private val locationRepo: LocationRepo,
    private val commitsRepo: CommitsRepo,
    private val waitingSyncDao: WaitingSyncDao,
    private val readSyncableDaos: ReadSyncableDaos,
    private val writeSyncableDaos: WriteSyncableDaos,
    private val prefs: SharedPreferences,
    private val ONLYFORDEBUGFULLDB: Daos,
) {
    fun heyAPIAlmighlyGodEtcCanIPleaseOnlyForDebugHaveAllDaoAccessImReallyTheDebugOnlyPleasePleasePlease() = ONLYFORDEBUGFULLDB
    fun getReadableDaosForWrapped() = readSyncableDaos
    private var lastUpdate = prefs.getLong("last_update", 0)
    fun resetLastUpdateDebug() { lastUpdate = 0 }
    private var isSyncing = false
    private val security = Security()
    private val logSyncableContent = true
    private fun optionallySkip(text: String?) = if(logSyncableContent) text else "[Skipped]"
    /** Returns the amount of entries that are updated, null on fail */
    suspend fun resync(): Int? {
        isSyncing = true
        val elementsPerRequest = 500
        var offset = 0
        Log.i(LOGTAG, "Sending entries to server...")
        while (true) {
            val lastNEntries = buildList {
                for (entry in waitingSyncDao.getLastNWaitingSyncEntries(
                    elementsPerRequest,
                    offset
                )) {
                    try {
                        val x = Syncable.from(entry, readSyncableDaos)
                        x?.let { add(it) }
                    } catch (e: Exception) {
                        Log.e(LOGTAG, "Failed to create Syncable from $entry", e)
                    }
                }
            }
            if (lastNEntries.isEmpty()) {
                Log.i(LOGTAG, "None to send")
                break
            }

            val json = JSONArray()
            for (entry in lastNEntries) {
                entry.toJson()?.also { json.put(it) }
                    ?: Log.w(LOGTAG, "Couldnt stringify ${entry.id} of type ${entry.calendarId} ($entry)")
            }

            Log.i(LOGTAG,"JSON:\n${optionallySkip(json.toString(2))}")
            val response = send(Method.Send, json.toString(), offset)
            Log.i(LOGTAG,"Server responded:\n${optionallySkip(response)}\n")
            if (response == null || response.isBlank()) {
                Log.e(LOGTAG, "Server didnt respond or threw an error (null or empty string reponse). We cant work with this!")
                isSyncing = false
                return null
            }
            val resJson = JSONObject(response)
            resJson
                .getJSONArray("msg")
                .forEach { Log.i(LOGTAG,"Server reported msg: $it") }
            resJson.getJSONArray("entries").jsonObjArray.forEach {
                waitingSyncDao.deleteWaitingSync(it.getId(), it.getInt("type"))
            }
            if (lastNEntries.size < elementsPerRequest) break
            Log.i(LOGTAG,"Server isn't done sending chunked responses...")
        }
        var serverLastUpdate: Long
        var updateAmount = 0
        offset = 0
        Log.i(LOGTAG,"Get new entries...")
        while (true) {
            val responseText = send(Method.Resync, "[]", offset)
            if (responseText == null) {
                isSyncing = false
                return null
            }
            if (!responseText.trim().startsWith("{")) {
                Log.w(LOGTAG,"API Unexpected response: $responseText current time: ${System.currentTimeMillis()}")
                isSyncing = false
                return null
            }
            val response = JSONObject(responseText)
            Log.i(LOGTAG, "Server entries (${responseText.length} bytes):\n${optionallySkip(response.toString(2))}}")
            val jsonArray = response.getJSONArray("e").jsonObjArray
            serverLastUpdate = response.getLong("date")
            if (jsonArray.isEmpty()) break
            updateAmount += jsonArray.size
            Log.i(LOGTAG, "Batch received ${jsonArray.size} events")
            jsonArray.forEach {
                Log.i(LOGTAG, "${optionallySkip(it.toString(2))}")
                overwriteByJson(it)
            }
            if (jsonArray.size < elementsPerRequest) break
            offset += elementsPerRequest
        }
        lastUpdate = serverLastUpdate
        prefs.edit {
            putLong("last_update", lastUpdate)
        }
        isSyncing = false
        return updateAmount
    }

    private suspend fun send(method: Method, data: String, offset: Int?): String? = security.send(
        "https://myxoz.de/life/_api.php",
        lastUpdate,
        System.currentTimeMillis(),
        data,
        method,
        offset
    )

    private suspend fun overwriteByJson(json: JSONObject) {
        val cal = json.getInt("type")
        when (cal) {
            in Int.MIN_VALUE..<0 -> {
                val id = json.getId()
                calendarRepo.deleteSyncedEventFromCache(id)
                writeSyncableDaos.eventDetailsDao.removeEventById(id)
                when (json.getInt("type")) { // GOTO AEFL
                    EventType.Sleep.id -> {}
                    EventType.Spont.id -> {
                        writeSyncableDaos.eventDetailsDao.removeSpont(id)
                        writeSyncableDaos.eventDetailsDao.removeTags(id)
                    }

                    EventType.Hobby.id -> {
                        writeSyncableDaos.eventDetailsDao.removeTags(id)
                        writeSyncableDaos.eventDetailsDao.removeHobby(id)
                    }

                    EventType.Work.id -> {
                        writeSyncableDaos.eventDetailsDao.removeTags(id)
                        writeSyncableDaos.eventDetailsDao.removeWork(id)
                    }

                    EventType.Learn.id -> {
                        writeSyncableDaos.eventDetailsDao.removeTags(id)
                        writeSyncableDaos.eventDetailsDao.removeLearn(id)
                    }

                    EventType.Social.id -> {
                        writeSyncableDaos.eventDetailsDao.removeTags(id)
                        writeSyncableDaos.eventDetailsDao.removeSocial(id)
                        writeSyncableDaos.eventDetailsDao.deletePeopleMapping(id)
                    }

                    EventType.DigSoc.id -> {
                        writeSyncableDaos.eventDetailsDao.removeDigSoc(id)
                        writeSyncableDaos.eventDetailsDao.deleteDigSocMapping(id)
                        writeSyncableDaos.eventDetailsDao.deletePeopleMapping(id)
                    }
                }
            }

            Syncable.SpecialSyncablesIds.BANKING -> {
                val new = BankingSyncable.Companion.overwriteDBByJson(writeSyncableDaos, json)
                bankingRepo.updateTransaction(
                    BankingRepo.BankingDisplayEntity.from(new.entity, readSyncableDaos.bankingDao)
                )
            }

            Syncable.SpecialSyncablesIds.DAYS -> {
                val new = FullDaySyncable.overwriteDBByJson(writeSyncableDaos, json)
                daySummaryRepo.setDaySummary(new)
            }

            Syncable.SpecialSyncablesIds.PEOPLE -> {
                val new = PersonSyncable.Companion.overwriteDBByJson(writeSyncableDaos, json)
                peopleRepo.updateCacheOnly(new)
            }

            Syncable.SpecialSyncablesIds.LOCATIONS -> {
                val new = LocationSyncable.Companion.overwriteDBByJson(writeSyncableDaos, json)
                locationRepo.update(new)
            }

            Syncable.SpecialSyncablesIds.BANKINGSIDECAR -> {
                val new = BankingSidecarSyncable.Companion.overwriteDBByJson(writeSyncableDaos, json)
                val displayEntity = BankingRepo.BankingDisplayEntity.from(
                    new.transactionId, readSyncableDaos.bankingDao
                )
                if(displayEntity!=null){ // Else Transactiton isnt yet synced
                    bankingRepo.updateTransaction(displayEntity)
                }
            }

            Syncable.SpecialSyncablesIds.PROFILEPICTURE -> {
                val pp = ProfilePictureSyncable.Companion.overwriteDBByJson(writeSyncableDaos, json)
                peopleRepo.updatePP(pp)
            }
            Syncable.SpecialSyncablesIds.COMMITS -> {
                val commit = CommitSyncable.Companion.overwriteDBByJson(writeSyncableDaos, json)
                commitsRepo.updateCommit(commit)
            }

            else -> {
                val syned = SyncedEvent.overwriteDBByJson(writeSyncableDaos, json)
                calendarRepo.updateSyncedEventCached(syned)
            }
        }
    }

    suspend fun testSign() = security.testSign(
        "https://myxoz.de/life/_api.php",
        lastUpdate,
        System.currentTimeMillis(),
    )

    enum class Method(val method: String) {
        Resync("r"),
        Send("s"),
        Verify("v")
    }

    companion object {
        const val LOGTAG = "API"
        fun generateId(): Long {
            return Random.Default.nextLong(0, Long.MAX_VALUE)
        }
    }
    class ReadSyncableDaos(
        val eventDetailsDao: ReadEventDetailsDao,
        val peopleDao: ReadPeopleDao,
        val locationsDao: ReadLocationsDao,
        val daysDao: ReadDaysDao,
        val bankingDao: ReadBankingDao,
        val commitsDao: ReadCommitsDao,
    )
    class WriteSyncableDaos(
        val eventDetailsDao: WriteEventDetailsDao,
        val peopleDao: WritePeopleDao,
        val locationsDao: WriteLocationsDao,
        val daysDao: WriteDaysDao,
        val bankingDao: WriteBankingDao,
        val commitsDao: WriteCommitsDao,
    )
    fun getBase64Public() = security.getBase64Public()
}