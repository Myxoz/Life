package com.myxoz.life.storage.dbwrapper.days

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
internal interface WriteDaysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(days: DaysEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDayScreenTime(dayScreenTime: List<DayScreenTimeEntity>)
}