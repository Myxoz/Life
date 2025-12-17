package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "fullname") val fullname: String?,

    @ColumnInfo(name = "phone_number") val phoneNumber: String?,

    @ColumnInfo(name = "iban") val iban: String?,

    @ColumnInfo(name = "home") val home: Long?,

    @ColumnInfo(name = "birthday") val birthday: Long?,
)

@Dao
interface PeopleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PersonEntity>)

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE id IN (:ids)")
    suspend fun getPersonsByIds(ids: List<Long>): List<PersonEntity>

    @Query("SELECT * FROM events AS e INNER JOIN people_mapping AS pm ON e.id = pm.event_id WHERE pm.person_id = :personId AND e.ends < :now ORDER BY e.ends DESC LIMIT 1")
    suspend fun getLastInteractionByPerson(personId: Long, now: Long): EventEntity?

    @Query("SELECT * FROM events AS e INNER JOIN people_mapping AS pm ON e.id = pm.event_id WHERE pm.person_id = :personId AND e.start > :now ORDER BY e.ends DESC LIMIT 1")
    suspend fun getNextPlanedEvent(personId: Long, now: Long): EventEntity?

    @Query("SELECT * FROM people")
    suspend fun getAllPeople(): List<PersonEntity>

    @Delete
    suspend fun deletePerson(person: PersonEntity)
}
