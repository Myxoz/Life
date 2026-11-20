package com.myxoz.life.storage.dbwrapper

internal class Daos(db: AppDatabase) {
    val localEventsDao = db.localEventsDao()
    val readExtensionDao = db.readExtensionDao()
    val writeExtensionDao = db.writeExtensionDao()
    val readTodosDao = db.readTodosDao()
    val writeTodosDao = db.writeTodosDao()
    val readEventDetailsDao = db.readEventsDetailsDao()
    val writeEventDetailsDao = db.writeEventDetailsDao()

    val readBankingDao = db.readBankingDao()
    val writeBankingDao = db.writeBankingDao()

    val readPeopleDao = db.readPeopleDao()
    val writePeopleDao = db.writePeopleDao()

    val readLocationsDao = db.readLocationsDao()
    val writeLocationsDao = db.writeLocationsDao()

    val readCommitsDao = db.readCommitsDao()
    val writeCommitsDao = db.writeCommitsDao()

    val readDaysDao = db.readDaysDao()
    val writeDaysDao = db.writeDaysDao()

    val waitingSync = db.waitingSyncDao()
    val proposedSteps = db.proposedStepsDao()
    val cleanupDb = db.databaseCleanupDao()
}