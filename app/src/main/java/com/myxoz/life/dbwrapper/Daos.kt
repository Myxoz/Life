package com.myxoz.life.dbwrapper

class Daos(db: AppDatabase) {
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

    val waitingSync = db.waitingSyncDao()
    val readDaysDao = db.readDaysDao()
    val writeDaysDao = db.writeDaysDao()
    val proposedSteps = db.proposedStepsDao()
    val cleanupDb = db.databaseCleanupDao()
}