package com.myxoz.life.dbwrapper.days

import androidx.room.Dao
import androidx.room.Query
import com.myxoz.life.dbwrapper.DayScreenTimeEntity

@Dao
interface ReadDaysDao {
    @Query("SELECT * FROM days WHERE date = :epochDay LIMIT 1")
    suspend fun getDay(epochDay: Long): DaysEntity?

    @Query("SELECT * FROM days ORDER BY date DESC LIMIT :n")
    suspend fun getLastNDays(n: Int): List<DaysEntity>

    @Query("SELECT * FROM days WHERE date > :start AND date < :ends")
    suspend fun getDaysBetween(start: Long, ends: Long): List<DaysEntity>

    @Query("SELECT * FROM dayscreentime WHERE date = :date")
    suspend fun getScreenTimesByDay(date: Long): List<DayScreenTimeEntity>

    @Query("SELECT * FROM dayscreentime WHERE date > :start AND date < :ends")
    suspend fun getScreenTimesByRange(start: Long, ends: Long): List<DayScreenTimeEntity>
}