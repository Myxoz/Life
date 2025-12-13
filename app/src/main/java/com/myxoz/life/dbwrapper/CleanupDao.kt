package com.myxoz.life.dbwrapper

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DatabaseCleanupDao {
    @Query("DELETE FROM banking")
    suspend fun clearBanking()

    @Query("DELETE FROM bankingsidecar")
    suspend fun clearBankingSidecar()

    @Query("DELETE FROM location")
    suspend fun clearLocations()

    @Query("DELETE FROM learn")
    suspend fun clearLearnCalendar()

    @Query("DELETE FROM social")
    suspend fun clearSocialCalendar()

    @Query("DELETE FROM hobby")
    suspend fun clearHobbyCalendar()

    @Query("DELETE FROM days")
    suspend fun clearFullDaySummeries()

    @Query("DELETE FROM tags")
    suspend fun clearEventTags()

    @Query("DELETE FROM dayscreentime")
    suspend fun clearFullDayScreentime()

    @Query("DELETE FROM people_mapping")
    suspend fun clearPeopleSocialMapping()

    @Query("DELETE FROM people")
    suspend fun clearKnownPeople()

    @Query("DELETE FROM spont")
    suspend fun clearSpontEvents()

    @Query("DELETE FROM events")
    suspend fun clearAllEvents()

    @Query("DELETE FROM travel")
    suspend fun clearTravelEvents()

    @Query("DELETE FROM vehicle")
    suspend fun clearVehicles()

    @Query("DELETE FROM profilepicture")
    suspend fun clearProfilePictureKnowledge()

    @Query("DELETE FROM digsocmapping")
    suspend fun clearDigSocMapping()

    @Query("DELETE FROM digsoc")
    suspend fun clearDigSoc()
    @Transaction
    suspend fun clearAllExceptPersistent() {
        clearBanking()
        clearBankingSidecar()
        clearLocations()
        clearLearnCalendar()
        clearSocialCalendar()
        clearHobbyCalendar()
        clearFullDaySummeries()
        clearEventTags()
        clearFullDayScreentime()
        clearPeopleSocialMapping()
        clearKnownPeople()
        clearSpontEvents()
        clearAllEvents()
        clearTravelEvents()
        clearVehicles()
        clearProfilePictureKnowledge()
        clearDigSoc()
        clearDigSocMapping()
    }
}