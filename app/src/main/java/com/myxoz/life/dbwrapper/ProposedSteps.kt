package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "proposed_steps")
data class ProposedStepsEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "day") val day: Long,

    @ColumnInfo(name = "steps") val steps: Int,
)

@Dao
interface ProposedStepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(entry: ProposedStepsEntity)

    @Query("SELECT * FROM proposed_steps WHERE day = :day LIMIT 1")
    suspend fun getStepsByDay(day: Long): ProposedStepsEntity?

    @Query("DELETE FROM proposed_steps WHERE day = :day")
    suspend fun deleteDay(day: Long)

    @Query("DELETE FROM proposed_steps WHERE 1")
    suspend fun clearAll()
}
