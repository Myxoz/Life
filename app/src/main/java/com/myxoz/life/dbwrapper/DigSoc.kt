package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "digsoc")
data class DigSocEntity(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,
)

@Dao
interface DigSocDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DigSocEntity)

    @Query("SELECT * FROM digsoc WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): DigSocEntity?

    @Query("DELETE FROM digsoc WHERE id = :id")
    suspend fun removeById(id: Long)
}
