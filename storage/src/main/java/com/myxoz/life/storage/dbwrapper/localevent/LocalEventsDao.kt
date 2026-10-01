package com.myxoz.life.storage.dbwrapper.localevent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface LocalEventsDao {
    @Query("SELECT * FROM localevent WHERE id = :id")
    suspend fun getLocalEventById(id: Long): LocalEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalEvent(localEventEntity: LocalEventEntity)

    @Query("DELETE FROM localevent WHERE id = :id")
    suspend fun deleteLocalEvent(id: Long)

    @Query("SELECT * FROM localevent WHERE ends > :start AND :end > start")
    suspend fun getLocalEventsBetween(start: Long, end: Long): List<LocalEventEntity>
}