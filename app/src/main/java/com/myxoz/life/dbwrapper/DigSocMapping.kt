package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "digsocmapping", primaryKeys = ["event_id", "app"])
data class DigSocMappingEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,

    @ColumnInfo(name = "app") val app: Int,

    @ColumnInfo(name = "duration_ms") val durationMs: Long,
)

@Dao
interface DigSocMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: DigSocMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mapping: List<DigSocMappingEntity>)

    @Query("SELECT * FROM digsocmapping WHERE event_id = :id")
    suspend fun getMappingByEventId(id: Long): List<DigSocMappingEntity>

    @Query("DELETE FROM digsocmapping WHERE event_id = :id")
    suspend fun deleteMappingByEventId(id: Long)
}
