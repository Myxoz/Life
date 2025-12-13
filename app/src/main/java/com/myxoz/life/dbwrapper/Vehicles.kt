package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "vehicle", primaryKeys = ["id", "type"])
data class VehicleEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "type") val type: Int,

    @ColumnInfo(name = "durationMs") val durationMs: Int,
)

@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)

    @Query("SELECT * FROM vehicle WHERE id = :id")
    suspend fun getEvent(id: Long): List<VehicleEntity>

    @Query("DELETE FROM vehicle WHERE id = :id")
    suspend fun removeById(id: Long)
}
