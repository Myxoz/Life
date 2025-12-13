package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "type") val type: Int,

    @ColumnInfo(name = "start") val start: Long,

    @ColumnInfo(name = "ends") val end: Long,

    @ColumnInfo(name = "created") val created: Long,

    @ColumnInfo(name = "edited") val edited: Long?,

    @ColumnInfo(name = "uss") val uss: Boolean,

    @ColumnInfo(name = "usl") val usl: Boolean
)

@Dao
interface EventsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE ends > :start AND :end > start")
    suspend fun getEventsBetween(start: Long, end: Long): List<EventEntity>

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("SELECT * FROM events WHERE ends > :rangeStart AND start < :rangeEnd")
    suspend fun getEventsOverlapping(
        rangeStart: Long,
        rangeEnd: Long
    ): List<EventEntity>

    @Query("SELECT e.* FROM events e INNER JOIN people_mapping pm ON pm.event_id = e.id WHERE  pm.person_id = :personId")
    suspend fun getEventsWithPerson(personId: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE start > :after AND ends < :endsBefore AND (type = 6 OR type = 8)")
    suspend fun getAllPeopleEvents(after: Long, endsBefore: Long): List<EventEntity>
}
