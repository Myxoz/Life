package com.myxoz.life.repositories

import android.app.Application
import com.myxoz.life.api.API
import com.myxoz.life.dbwrapper.Daos
import com.myxoz.life.dbwrapper.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainApplication: Application() {
    private lateinit var db: Daos
    lateinit var repositories: AppRepositories
        private set

    override fun onCreate() {
        super.onCreate()
        db = Daos(DatabaseProvider.getDatabase(applicationContext))
        val readSyncableDaos = API.ReadSyncableDaos(
            db.readEventDetailsDao,
            db.readPeopleDao,
            db.readLocationsDao,
            db.readDaysDao,
            db.readBankingDao,
            db.readCommitsDao,
            db.readTodosDao
        )
        val writeSyncableDaos = API.WriteSyncableDaos(
            db.writeEventDetailsDao,
            db.writePeopleDao,
            db.writeLocationsDao,
            db.writeDaysDao,
            db.writeBankingDao,
            db.writeCommitsDao,
            db.writeTodosDao
        )
        val mainPrefs = applicationContext.getSharedPreferences(SPK, MODE_PRIVATE)
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val bankingRepo = BankingRepo(
            readSyncableDaos.bankingDao,
            writeSyncableDaos,
            mainPrefs,
            appScope,
            db.waitingSync,
        )
        val peopleRepo = PeopleRepo(
            readSyncableDaos,
            writeSyncableDaos,
            db.waitingSync,
            applicationContext,
            appScope
        )
        val calendarRepo = CalendarRepo(
            readSyncableDaos,
            writeSyncableDaos,
            db.waitingSync,
            // applicationContext.getSharedPreferences("autodetect", MODE_PRIVATE),
            applicationContext,
            appScope
        )
        val daySummaryRepo = DaySummaryRepo(
            readSyncableDaos,
            writeSyncableDaos,
            db.waitingSync,
            appScope
        )
        val locationRepo = LocationRepo(
            readSyncableDaos,
            writeSyncableDaos,
            db.waitingSync,
            appScope
        )
        val contactRepo = ContactRepo(
            applicationContext,
            appScope
        )
        val commitsRepo = CommitsRepo(
            readSyncableDaos,
            appScope
        )
        val aiPredictionRepo = AIPredictionRepo(
            applicationContext,
            readSyncableDaos,
            appScope
        )
        val todosRepo = TodoRepo(
            readSyncableDaos.todosDao,
            writeSyncableDaos,
            appScope,
            db.waitingSync
        )
        val stepRepo = StepRepo(db.proposedSteps, applicationContext.getSharedPreferences("steps", MODE_PRIVATE), appScope)
        repositories = AppRepositories(
            calendarRepo,
            daySummaryRepo,
            stepRepo,
            peopleRepo,
            commitsRepo,
            contactRepo,
            bankingRepo,
            locationRepo,
            aiPredictionRepo,
            todosRepo,
            API(
                calendarRepo,
                daySummaryRepo,
                peopleRepo,
                bankingRepo,
                locationRepo,
                commitsRepo,
                todosRepo,
                db.waitingSync,
                readSyncableDaos,
                writeSyncableDaos,
                mainPrefs,
                db,
                applicationContext
            ),
            readSyncableDaos,
            mainPrefs,
            applicationContext
        )
    }
    companion object {
        const val SPK = "MainActivity"
    }
}