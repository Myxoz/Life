package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "work")
data class WorkEntity(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,

    @ColumnInfo(name = "details") val details: String?,
)

@Dao
interface WorkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: WorkEntity)

    @Query("SELECT * FROM work WHERE id = :id LIMIT 1")
    suspend fun getWork(id: Long): HobbyEntiy?

    @Query("DELETE FROM work WHERE id = :id")
    suspend fun removeById(id: Long)
}
