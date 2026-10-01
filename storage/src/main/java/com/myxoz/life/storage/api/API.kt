package com.myxoz.life.storage.api

import android.content.Context
import android.util.Log
import com.myxoz.life.storage.api.helper.MixedBankingEntity
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
import com.myxoz.life.storage.api.syncables.ServerSyncable
import com.myxoz.life.storage.api.syncables.SyncableIds.getCalendarId
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.api.syncables.TransactionSplitSyncable
import com.myxoz.life.storage.convertion.RemoveSyncableFromDatabase
import com.myxoz.life.storage.convertion.SavePersistent
import com.myxoz.life.storage.convertion.SyncableFromDatabase
import com.myxoz.life.storage.convertion.SyncableFromJSON
import com.myxoz.life.storage.convertion.SyncableToJSON
import com.myxoz.life.storage.dbwrapper.Daos
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao
import com.myxoz.life.storage.dbwrapper.WaitingSyncEntity
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.storage.dbwrapper.banking.WriteBankingDao
import com.myxoz.life.storage.dbwrapper.commits.ReadCommitsDao
import com.myxoz.life.storage.dbwrapper.commits.WriteCommitsDao
import com.myxoz.life.storage.dbwrapper.days.ReadDaysDao
import com.myxoz.life.storage.dbwrapper.days.WriteDaysDao
import com.myxoz.life.storage.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.storage.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.storage.dbwrapper.extension.ReadExtensionDao
import com.myxoz.life.storage.dbwrapper.extension.WriteExtensionDao
import com.myxoz.life.storage.dbwrapper.locations.ReadLocationsDao
import com.myxoz.life.storage.dbwrapper.locations.WriteLocationsDao
import com.myxoz.life.storage.dbwrapper.people.ReadPeopleDao
import com.myxoz.life.storage.dbwrapper.people.WritePeopleDao
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import com.myxoz.life.storage.dbwrapper.todos.ReadTodosDao
import com.myxoz.life.storage.dbwrapper.todos.WriteTodosDao
import com.myxoz.life.storage.interfaces.BankingInterface
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.CommitsInterface
import com.myxoz.life.storage.interfaces.DaySummaryInterface
import com.myxoz.life.storage.interfaces.ExtensionInterface
import com.myxoz.life.storage.interfaces.LocationInterface
import com.myxoz.life.storage.interfaces.PeopleInterface
import com.myxoz.life.storage.interfaces.TodoInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import kotlin.random.Random

class API internal constructor(
    private val hooks: UpdateHooks,
    private val calendarInterface: CalendarInterface,
    private val daySummaryInterface: DaySummaryInterface,
    private val peopleInterface: PeopleInterface,
    private val bankingInterface: BankingInterface,
    private val locationInterface: LocationInterface,
    private val commitsInterface: CommitsInterface,
    private val todoInterface: TodoInterface,
    private val extensionInterface: ExtensionInterface,
    private val waitingSyncDao: WaitingSyncDao,
    private val readSyncableDaos: ReadSyncableDaos,
    private val writeSyncableDaos: WriteSyncableDaos,
    private val keyValueStore: KeyValueStore,
    private val ONLYFORDEBUGFULLDB: Daos,
    private val context: Context
) {
    private var lastUpdate by keyValueStore.getAsState(lastUpdateKey, Instant.EPOCH)
    internal fun resetLastUpdateDebug() {
        lastUpdate = Instant.EPOCH
    }
    private val security = Security()
    private val logSyncableContent = true
    private fun optionallySkip(text: String?) = if(logSyncableContent) text else "[Skipped]"
    sealed interface SyncingResponse {
        class FAILED(val reason: String): SyncingResponse
        object OFFLINE: SyncingResponse
        class SUCCESS(val changed: Boolean = true): SyncingResponse
    }
    private suspend fun sendToServer(): SyncingResponse {
        var offset = 0
        val fullySyncedWaitingEntries = mutableListOf<WaitingSyncEntity>()
        var sendSyncables = 0
        Log.i(LOGTAG, "Sending entries to server...")
        while (true) {
            val syncables = SyncableFromDatabase.fromWaitingSync(
                waitingSyncDao.getLastNWaitingSyncEntries(ELEMENTSPERREQUEST, offset),
                readSyncableDaos,
                context
            )
            if (syncables.isEmpty()) {
                Log.i(LOGTAG, "None to send ()")
                break
            }

            val json = LifeJSONArray.from(syncables) {
                SyncableToJSON.toJSON(it)
            }

            Log.i(LOGTAG,"JSON:\n${optionallySkip(json.formatted())}")

            sendSyncables += json.length()
            val response = send(Method.Send, json.asString(), ELEMENTSPERREQUEST) ?:
                return SyncingResponse.OFFLINE

            Log.i(LOGTAG,"Server responded:\n${optionallySkip(response)}\n")
            val resJson = LifeJSONObject.safeFrom(response)
            if (response.isBlank() || resJson == null) {
                Log.e(LOGTAG, "Server didnt respond or threw an error (null or empty string reponse). We cant work with this!")
                return SyncingResponse.FAILED(response)
            }
            resJson
                .getJSONArray("msg")
                .forEach { Log.w(LOGTAG,"Server reported msg: $it") }
            fullySyncedWaitingEntries += resJson.getJSONArray("entries").mapJSONObjects {
                WaitingSyncEntity(it.getId(), it.getType(), 0L)
            }
            if (syncables.size < ELEMENTSPERREQUEST) break
            Log.i(LOGTAG,"Sending more chunked data to server ...")
            offset += ELEMENTSPERREQUEST
        }
        waitingSyncDao.deleteWaitingSyncItems(fullySyncedWaitingEntries)
        return SyncingResponse.SUCCESS(offset != 0)
    }
    private suspend fun receiveFromServer(): SyncingResponse {
        var serverLastUpdate: Instant
        var offset = 0
        var receivedElements = 0
        Log.i(LOGTAG,"Get new entries...")
        while (true) {
            val responseText = send(Method.Resync, "[]", offset) ?:
                return SyncingResponse.OFFLINE
            val response = LifeJSONObject.safeFrom((responseText))
            Log.i(LOGTAG, "Server entries (${responseText.length} bytes):\n${optionallySkip(response?.formatted())}}")
            if (response == null) {
                Log.w(LOGTAG,"API Unexpected response: \"$responseText\". Current time: ${System.currentTimeMillis()}")
                return SyncingResponse.FAILED(responseText)
            }
            val jsonArray = response.getJSONArray("e")
            serverLastUpdate = response.getInstant("date")
            if (jsonArray.isEmpty()) break
            Log.i(LOGTAG, "Batch received ${jsonArray.length()} events")
            val idList = jsonArray.jsonObjectArray().map { it.getType() to it.getId() }
            val oldSyncables = SyncableFromDatabase.from(idList, readSyncableDaos, context)
            val calendarMap = oldSyncables.associate { it.id to it.getCalendarId() }
            jsonArray.forEachJSONObjects {
                Log.i(LOGTAG, "${optionallySkip(it.formatted())}")
                val syncable = SyncableFromJSON.fromJSON(it)
                val oldCalendarId = calendarMap[it.getId()]
                val changedCalendarType = oldCalendarId != null && oldCalendarId != it.getType()
                RemoveSyncableFromDatabase.removeSyncable(
                    syncable, writeSyncableDaos, context,
                    changedCalendarType
                )
                SavePersistent.saveToPersistentStorage(
                    syncable, writeSyncableDaos, readSyncableDaos, context
                )
                updateCaches(syncable)
            }
            receivedElements += jsonArray.length()
            if (jsonArray.length() < ELEMENTSPERREQUEST) break
            offset += ELEMENTSPERREQUEST
        }
        lastUpdate = serverLastUpdate
        Log.i(LOGTAG, "Done with resyncing")
        return SyncingResponse.SUCCESS(receivedElements != 0)
    }
    suspend fun synchronizeWithServer(): SyncingResponse {
        val sendDataResponse = sendToServer()
        if(sendDataResponse !is SyncingResponse.SUCCESS) {
            return sendDataResponse
        }
        val receivedData = receiveFromServer()
        return receivedData
    }

    private suspend fun send(method: Method, data: String, offset: Int?): String? = security.send(
        "https://myxoz.de/life/_api.php",
        lastUpdate,
        Instant.now(),
        data,
        method,
        offset
    )
    suspend fun testSign() = TestAPI.testSign(
        security,
        "https://myxoz.de/life/_api.php",
        lastUpdate,
        Instant.now()
    )

    enum class Method(val method: String) {
        Resync("r"),
        Send("s"),
        Verify("v")
    }

    internal class ReadSyncableDaos(
        val eventDetailsDao: ReadEventDetailsDao,
        val peopleDao: ReadPeopleDao,
        val locationsDao: ReadLocationsDao,
        val daysDao: ReadDaysDao,
        val bankingDao: ReadBankingDao,
        val commitsDao: ReadCommitsDao,
        val todosDao: ReadTodosDao,
        val extensionDao: ReadExtensionDao,
    )
    internal class WriteSyncableDaos(
        val eventDetailsDao: WriteEventDetailsDao,
        val peopleDao: WritePeopleDao,
        val locationsDao: WriteLocationsDao,
        val daysDao: WriteDaysDao,
        val bankingDao: WriteBankingDao,
        val commitsDao: WriteCommitsDao,
        val todosDao: WriteTodosDao,
        val extensionDao: WriteExtensionDao,
    )
    fun getBase64Public() = security.getBase64Public()
    suspend fun updateCaches(new: ServerSyncable) {
        when(new) {
            is DeleteEntry -> {

            }
            is FullDaySyncable -> daySummaryInterface.updateCache(new)
            is PersonSyncable -> peopleInterface.updateCache(new)
            is BankingSyncable -> bankingInterface.updateCachedTransaction(
                new.id,
                MixedBankingEntity.from(new, readSyncableDaos.bankingDao)
            )
            is LocationSyncable -> locationInterface.updateCached(new)
            is ProfilePictureSyncable -> peopleInterface.updatePP(new)
            is BankingSidecarSyncable -> bankingInterface.updateCachedTransaction(
                new.transactionId,
                MixedBankingEntity.from(
                    new.transactionId, readSyncableDaos.bankingDao
                ) ?: return
            )
            is CommitSyncable -> commitsInterface.updateCommit(new)
            is TodoSyncable -> todoInterface.updateCachedTodo(new)
            is TransactionSplitSyncable -> bankingInterface.updateCachedSplit(new)
            is ManualTransactionSyncable -> bankingInterface.updateCachedManualTransaction(new)
            is ExtensionSyncable -> {
                hooks.updateExtension(new)
                extensionInterface.overwriteCache(new.type, new.content)
            }
            is SyncedEvent -> calendarInterface.updateSyncedEventCached(new)
        }
    }
    internal companion object {
        const val LOGTAG = "API"
        internal fun generateId(): Long = Random.nextLong(0, Long.MAX_VALUE)
        private const val ELEMENTSPERREQUEST = 500
        private val keyValueStoreKey = KeyValueStorePrefix("API")
        private val lastUpdateKey = keyValueStoreKey.keyOfType("last_update", KeyValueType.InstantType)
        internal val requestSync = MutableStateFlow<Instant>(Instant.EPOCH)
        fun requestSync() {
            requestSync.value = Instant.now()
        }
        val lastSyncRequiredTimestamp = requestSync.asStateFlow()
    }
}