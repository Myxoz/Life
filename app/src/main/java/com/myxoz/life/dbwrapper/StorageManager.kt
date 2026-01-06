package com.myxoz.life.dbwrapper

import android.content.SharedPreferences

class StorageManager(db: AppDatabase, sharedPrefs: SharedPreferences) {
    val work = db.workDao()
    val cleanupDb = db.databaseCleanupDao()
    val prefs = sharedPrefs
    val commits = db.commitsDao()
    val banking = db.bankingDao()
    val waitingSync = db.waitingSyncDao()
    val dayScreenTime = db.dayScreenTimeDao()
    val days = db.daysDao()
    val events = db.eventsDao()
    val proposedSteps = db.proposedStepsDao()
    val spont = db.spontDao()
    val tags = db.tagsDao()
    val learn = db.learnDao()
    val hobby = db.hobbyDao()
    val social = db.socialDao()
    val peopleMapping = db.peopleMappingDao()
    val bankingSidecar = db.bankingSideCarDao()
    val travel = db.travelDao()
    val location = db.locationDao()
    val vehicle = db.vehicleDao()
    val people = db.peopleDao()
    val profilePictureDao = db.profilePicture()
    val socials = db.socialsDao()
    val digsoc = db.digsocDao()
    val digsocMapping = db.digsocMappingDao()
}