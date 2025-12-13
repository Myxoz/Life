package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "people_mapping", primaryKeys = ["event_id", "person_id"])
data class PeopleMappingEntity(
    @ColumnInfo(name = "event_id") val eventId: Long,

    @ColumnInfo(name = "person_id") val personId: Long,
)

@Dao
interface PeopleMappingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PeopleMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PeopleMappingEntity>)

    @Query("SELECT * FROM people_mapping WHERE event_id = :id")
    suspend fun getMappingsByEventId(id: Long): List<PeopleMappingEntity>

    @Query("SELECT * FROM people_mapping AS p INNER JOIN events AS e WHERE e.id == p.event_id AND e.start > :start AND e.ends < :endsBefore AND (e.type = 8 OR e.type = 6)")
    suspend fun getAllSocialMappingAfter(start: Long, endsBefore: Long): List<PeopleMappingEntity>

    @Query("DELETE FROM people_mapping WHERE event_id = :id")
    suspend fun deleteMappingByEventId(id: Long)
}