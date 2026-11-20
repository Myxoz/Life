package com.myxoz.life.storage.interfaces

import android.content.Context
import android.util.Log
import com.myxoz.life.core.minus
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.api.UpdateHooks
import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.dbwrapper.Daos
import com.myxoz.life.storage.dbwrapper.DatabaseProvider
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.interfaces.aggregator.Aggregators
import com.myxoz.life.storage.interfaces.aggregator.AlarmAggregator
import com.myxoz.life.storage.interfaces.aggregator.LifeReschedulingAggregator
import com.myxoz.life.storage.interfaces.aggregator.PeopleEventAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class DatabaseInterface internal constructor(
    val hooks: UpdateHooks,
    val keyValueStore: KeyValueStore,
    val calendarInterface: CalendarInterface,
    val daySummaryInterface: DaySummaryInterface,
    val stepInterface: StepInterface,
    val peopleInterface: PeopleInterface,
    val commitsInterface: CommitsInterface,
    val bankingInterface: BankingInterface,
    val locationInterface: LocationInterface,
    val todoInterface: TodoInterface,
    val extensionInterface: ExtensionInterface,
    val debugInterface: DebugInterface,
    val api: API,
    val aggregators: Aggregators,
    val context: Context,
    val appScope: CoroutineScope
) {
    companion object {
        fun by(applicationContext: Context): DatabaseInterface {
            val db = Daos(DatabaseProvider.getDatabase(applicationContext))
            val readSyncableDaos = API.ReadSyncableDaos(
                db.readEventDetailsDao,
                db.readPeopleDao,
                db.readLocationsDao,
                db.readDaysDao,
                db.readBankingDao,
                db.readCommitsDao,
                db.readTodosDao,
                db.readExtensionDao
            )
            val writeSyncableDaos = API.WriteSyncableDaos(
                db.writeEventDetailsDao,
                db.writePeopleDao,
                db.writeLocationsDao,
                db.writeDaysDao,
                db.writeBankingDao,
                db.writeCommitsDao,
                db.writeTodosDao,
                db.writeExtensionDao
            )
            val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            Log.d(LOGTAG, "Ran before blocking. Check time from now: ${System.currentTimeMillis()}")
            val start = Instant.now()
            val keyValueStore = KeyValueStore.createWithContext(applicationContext)
            val end = Instant.now()
            val duration = start - end
            Log.d(LOGTAG, "Inital keyvaluestore read "+when(duration) {
                in 0.milliseconds..1.milliseconds -> "blazingly fast"
                in 1.milliseconds..5.milliseconds -> "pretty fast"
                in 5.milliseconds..10.milliseconds -> "decently fast"
                in 10.milliseconds..50.milliseconds -> "aceptably slow"
                in 50.milliseconds..100.milliseconds -> "pretty slow"
                in 100.milliseconds..300.milliseconds -> "worrying slow"
                else -> "too slow"
            }+" in $duration (at ${System.currentTimeMillis()})")
            val syncHelper = SyncHelper(
                db.waitingSync,
                applicationContext,
                writeSyncableDaos,
                readSyncableDaos,
            )
            val bankingInterface = BankingInterface(
                db.readBankingDao,
                appScope,
                syncHelper
            )
            val peopleInterface = PeopleInterface(
                db.readPeopleDao,
                applicationContext,
                appScope,
                syncHelper
            )
            val calendarInterface = CalendarInterface(
                readSyncableDaos.eventDetailsDao,
                db.localEventsDao,
                applicationContext,
                appScope,
                syncHelper
            )
            val daySummaryInterface = DaySummaryInterface(
                db.readDaysDao,
                appScope,
                syncHelper,
            )
            val locationInterface = LocationInterface(
                db.readLocationsDao,
                appScope,
                syncHelper
            )
            val commitsInterface = CommitsInterface(
                readSyncableDaos.commitsDao,
                appScope
            )
            val todoInterface = TodoInterface(
                db.readTodosDao,
                appScope,
                syncHelper
            )
            val extensionInterface = ExtensionInterface(
                appScope,
                db.readExtensionDao,
                syncHelper
            )
            val stepInterface = StepInterface(
                db.proposedSteps,
                keyValueStore,
                appScope
            )
            val hooks = UpdateHooks(
                calendarInterface,
                extensionInterface
            )
            val peopleEventAggregator = PeopleEventAggregator(
                calendarInterface,
                readSyncableDaos.peopleDao,
                readSyncableDaos.eventDetailsDao,
            )
            val lifeReschedulingAggregator = LifeReschedulingAggregator(
                keyValueStore,
                calendarInterface,
                db.readEventDetailsDao,
                locationInterface,
                peopleInterface
            )
            val alarmScheduler = AlarmAggregator(keyValueStore)
            val aggregators = Aggregators(
                peopleEventAggregator,
                lifeReschedulingAggregator,
                alarmScheduler
            )
            val api = API(
                hooks,
                calendarInterface,
                daySummaryInterface,
                peopleInterface,
                bankingInterface,
                locationInterface,
                commitsInterface,
                todoInterface,
                extensionInterface,
                db.waitingSync,
                readSyncableDaos,
                writeSyncableDaos,
                keyValueStore,
                db,
                applicationContext
            )
            val debugInterface = DebugInterface(
                api,
                keyValueStore,
                stepInterface,
                db,
                applicationContext
            )
            return DatabaseInterface(
                hooks,
                keyValueStore,
                calendarInterface,
                daySummaryInterface,
                stepInterface,
                peopleInterface,
                commitsInterface,
                bankingInterface,
                locationInterface,
                todoInterface,
                extensionInterface,
                debugInterface,
                api,
                aggregators,
                applicationContext,
                appScope
            )
        }
        const val LOGTAG = "INIT"
    }
}