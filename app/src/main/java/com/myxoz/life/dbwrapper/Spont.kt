package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "spont")
data class SpontEntiy(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,
)

@Dao
interface SpontDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(spont: SpontEntiy)

    @Query("SELECT * FROM spont WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: Long): SpontEntiy?

    @Query("DELETE FROM spont WHERE id = :id")
    suspend fun removeById(id: Long)
}
