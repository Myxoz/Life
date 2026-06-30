package com.myxoz.life.storage.interfaces

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.myxoz.life.aggregator.AppAggregators
import com.myxoz.life.api.API
import com.myxoz.life.api.UpdateHooks
import com.myxoz.life.repositories.DeviceContactRepo
import com.myxoz.life.storage.dbwrapper.Daos
import com.myxoz.life.storage.dbwrapper.DatabaseProvider
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DatabaseInterface(
    val hooks: UpdateHooks,
    val calendarInterface: CalendarInterface,
    val daySummaryInterface: DaySummaryInterface,
    val stepInterface: StepInterface,
    val peopleInterface: PeopleInterface,
    val commitsInterface: CommitsInterface,
    val deviceContactRepo: DeviceContactRepo,
    val bankingRepo: BankingRepo,
    val locationInterface: LocationInterface,
    val aiPredictionRepo: AIPredictionRepo,
    val todoInterface: TodoInterface,
    val extensionInterface: ExtensionInterface,
    val api: API,
    val readSyncableDaos: API.ReadSyncableDaos,
    val prefs: SharedPreferences,
    val permissionChecker: Settings.Permission.PermissionChecker,
    val context: Context,
    val appScope: CoroutineScope
) {
    val aggregators = AppAggregators(this)
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
            val mainPrefs = applicationContext.getSharedPreferences(SPK, MODE_PRIVATE)
            val settingsPrefs = applicationContext.getSharedPreferences(SETTINGS_SPK, MODE_PRIVATE)
            val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val bankingRepo = BankingRepo(
                readSyncableDaos.bankingDao,
                writeSyncableDaos,
                appScope,
                db.waitingSync,
            )
            val peopleInterface = PeopleInterface(
                readSyncableDaos,
                writeSyncableDaos,
                db.waitingSync,
                applicationContext,
                appScope
            )
            val calendarInterface = CalendarInterface(
                readSyncableDaos,
                writeSyncableDaos,
                db.waitingSync,
                // applicationContext.getSharedPreferences("autodetect", MODE_PRIVATE),
                applicationContext,
                appScope
            )
            val daySummaryInterface = DaySummaryInterface(
                readSyncableDaos,
                writeSyncableDaos,
                db.waitingSync,
                appScope
            )
            val locationInterface = LocationInterface(
                readSyncableDaos,
                writeSyncableDaos,
                db.waitingSync,
                appScope
            )
            val deviceContactRepo = DeviceContactRepo(
                applicationContext,
                appScope
            )
            val commitsInterface = CommitsInterface(
                readSyncableDaos,
                appScope
            )
            val aiPredictionRepo = AIPredictionRepo(
                applicationContext,
                readSyncableDaos,
                appScope
            )
            val todosRepo = TodoInterface(
                readSyncableDaos.todosDao,
                writeSyncableDaos,
                appScope,
                db.waitingSync
            )
            val extensionInterface = ExtensionInterface(
                appScope,
                readSyncableDaos.extensionDao,
                writeSyncableDaos.extensionDao,
                writeSyncableDaos,
                db.waitingSync
            )
            val stepInterface = StepInterface(
                db.proposedSteps,
                applicationContext.getSharedPreferences("steps", MODE_PRIVATE),
                appScope
            )
            val hooks = UpdateHooks(
                calendarInterface,
                extensionInterface
            )
            return DatabaseInterface(
                hooks,
                calendarInterface,
                daySummaryInterface,
                stepInterface,
                peopleInterface,
                commitsInterface,
                deviceContactRepo,
                bankingRepo,
                locationInterface,
                aiPredictionRepo,
                todosRepo,
                extensionInterface,
                API(
                    hooks,
                    calendarInterface,
                    daySummaryInterface,
                    peopleInterface,
                    bankingRepo,
                    locationInterface,
                    commitsInterface,
                    todosRepo,
                    extensionInterface,
                    db.waitingSync,
                    readSyncableDaos,
                    writeSyncableDaos,
                    mainPrefs,
                    db,
                    applicationContext
                ),
                readSyncableDaos,
                mainPrefs,
                Settings.Permission.PermissionChecker(settingsPrefs, applicationContext),
                applicationContext,
                appScope
            )
        }
        const val SPK = "MainActivity"
        const val SETTINGS_SPK = "settings"
    }
}