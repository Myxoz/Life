package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "travel")
data class TravelEntity(
    @PrimaryKey(false)

    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "from") val from: Long,

    @ColumnInfo(name = "to") val to: Long,
)

@Dao
interface TravelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(travel: TravelEntity)

    @Query("SELECT * FROM travel WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: Long): TravelEntity?

    @Query("DELETE FROM travel WHERE id = :id")
    suspend fun removeById(id: Long)
}
