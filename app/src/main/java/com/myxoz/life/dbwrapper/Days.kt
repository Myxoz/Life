package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "days")
data class DaysEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "date") val id: Int,

    @ColumnInfo(name = "screen_time_ms") val screenTimeMs: Int,

    @ColumnInfo(name = "steps") val steps: Int,

    @ColumnInfo(name = "happyness") val happyness: Int,

    @ColumnInfo(name = "stress") val stress: Int,

    @ColumnInfo(name = "successfulness") val successfulness: Int,
)

@Dao
interface DaysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(days: DaysEntity)

    @Query("SELECT * FROM days WHERE date = :epochDay LIMIT 1")
    suspend fun getDay(epochDay: Int): DaysEntity?

    @Query("SELECT * FROM days ORDER BY date DESC LIMIT :n")
    suspend fun getLastNDays(n: Int): List<DaysEntity>
}