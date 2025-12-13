package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "learn")
data class LearnEntity(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,

    @ColumnInfo(name = "details") val details: String?,
)

@Dao
interface LearnDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(learnEntity: LearnEntity)

    @Query("SELECT * FROM learn WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: Long): LearnEntity?

    @Query("DELETE FROM learn WHERE id = :id")
    suspend fun removeById(id: Long)
}
