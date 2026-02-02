package com.myxoz.life.dbwrapper.days

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.myxoz.life.dbwrapper.DayScreenTimeEntity

@Dao
interface WriteDaysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(days: DaysEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDayScreenTime(dayScreenTime: List<DayScreenTimeEntity>)
}