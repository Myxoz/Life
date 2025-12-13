package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "waitingsync", primaryKeys = ["id", "calendar"])
data class WaitingSyncEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "calendar") val calendar: Int,

    @ColumnInfo(name = "date") val date: Long,
)

@Dao
interface WaitingSyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaitingSync(waitingSync: WaitingSyncEntity)

    @Query("SELECT * FROM waitingsync ORDER BY date ASC LIMIT :n OFFSET :offset")
    suspend fun getLastNWaitingSyncEntries(n: Int, offset: Int): List<WaitingSyncEntity>

    @Query("DELETE FROM waitingsync WHERE id = :id AND calendar = :calendar")
    suspend fun deleteWaitingSync(id: Long, calendar: Int)
}