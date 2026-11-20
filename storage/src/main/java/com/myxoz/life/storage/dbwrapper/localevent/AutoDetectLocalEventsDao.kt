package com.myxoz.life.storage.dbwrapper.localevent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface AutoDetectLocalEventsDao {
    @Query("SELECT * FROM autodetectlocalevent WHERE id = :id")
    suspend fun getAutoDetectLocalEventById(id: Long): AutoDetectLocalEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoDetectLocalEvent(autoDetectLocalEventEntity: AutoDetectLocalEventEntity)

    @Query("DELETE FROM autodetectlocalevent WHERE id = :id")
    suspend fun deleteLocalEvent(id: Long)

    @Query("SELECT * FROM autodetectlocalevent WHERE ends > :start AND :end > start")
    suspend fun getAutoDetectLocalEventsBetween(start: Long, end: Long): List<AutoDetectLocalEventEntity>
}