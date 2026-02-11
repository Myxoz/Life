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

    val spk = "MainActivity"
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
        )
        val writeSyncableDaos = API.WriteSyncableDaos(
            db.writeEventDetailsDao,
            db.writePeopleDao,
            db.writeLocationsDao,
            db.writeDaysDao,
            db.writeBankingDao,
            db.writeCommitsDao
        )
        val mainPrefs = applicationContext.getSharedPreferences(spk, MODE_PRIVATE)
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val bankingRepo = BankingRepo(
            readSyncableDaos.bankingDao,
            mainPrefs,
            appScope
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
            API(
                calendarRepo,
                daySummaryRepo,
                peopleRepo,
                bankingRepo,
                locationRepo,
                commitsRepo,
                db.waitingSync,
                readSyncableDaos,
                writeSyncableDaos,
                mainPrefs,
                db
            ),
            readSyncableDaos,
            mainPrefs,
            applicationContext
        )
    }
}