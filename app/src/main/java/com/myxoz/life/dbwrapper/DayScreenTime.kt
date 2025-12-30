package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "dayscreentime", primaryKeys = ["date", "packagename"])
data class DayScreenTimeEntity(
    @ColumnInfo(name = "date") val date: Long,

    @ColumnInfo(name = "packagename") val packagename: String,

    @ColumnInfo(name = "duration") val duration: Long,
)

@Dao
interface DayScreenTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDayScreenTime(dayScreenTime: DayScreenTimeEntity)

    @Query("SELECT * FROM dayscreentime WHERE date = :date")
    suspend fun getScreenTimesByDay(date: Long): List<DayScreenTimeEntity>

    @Query("SELECT * FROM dayscreentime WHERE date > :start AND date < :ends")
    suspend fun getScreenTimesByRange(start: Long, ends: Long): List<DayScreenTimeEntity>
}