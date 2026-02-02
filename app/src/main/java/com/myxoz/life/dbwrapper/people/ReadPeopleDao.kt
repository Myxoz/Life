package com.myxoz.life.dbwrapper.people

import androidx.room.Dao
import androidx.room.Query
import com.myxoz.life.dbwrapper.events.EventEntity
import java.time.LocalDate
import java.time.MonthDay

@Dao
interface ReadPeopleDao {
    @Query("SELECT * FROM people WHERE iban = :iban LIMIT 1")
    suspend fun getPersonByIban(iban: String): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE id IN (:ids)")
    suspend fun getPersonsByIds(ids: List<Long>): List<PersonEntity>

    @Query("SELECT * FROM events AS e INNER JOIN people_mapping AS pm ON e.id = pm.event_id WHERE pm.person_id = :personId AND e.ends < :now ORDER BY e.ends DESC LIMIT 1")
    suspend fun getLastInteractionByPerson(personId: Long, now: Long): EventEntity?

    @Query("SELECT * FROM events AS e INNER JOIN people_mapping AS pm ON e.id = pm.event_id WHERE pm.person_id = :personId AND e.start > :now ORDER BY e.ends DESC LIMIT 1")
    suspend fun getNextInteractionByPerson(personId: Long, now: Long): EventEntity?

    @Query("SELECT * FROM people")
    suspend fun getAllPeople(): List<PersonEntity>

    @Query("""SELECT * FROM events e INNER JOIN people_mapping AS pm ON e.id = pm.event_id WHERE pm.person_id IN (:people) AND e.start = (
            SELECT MIN(e2.start) FROM events e2 JOIN people_mapping pm2 ON e2.id = pm2.event_id WHERE pm2.person_id = pm.person_id)""")
    suspend fun getFirstEventsFor(people: List<Long>): List<EventEntity>

    suspend fun getPeopleWithBirthdayAt(date: LocalDate): List<PersonEntity> {
        val monthDay = MonthDay.from(date)
        val year = date.year
        return peopleWithBirthdaysIn(
            (0..130).mapNotNull { offset ->
                val year = year-offset
                monthDay.atYear(year).takeIf { it.monthValue == monthDay.monthValue }?.toEpochDay()
            }
        )
    }

    @Query("SELECT * FROM people WHERE birthday IN (:dates)")
    suspend fun peopleWithBirthdaysIn(dates: List<Long>): List<PersonEntity>

    // PP (:P)
    @Query("SELECT * FROM profilepicture WHERE person_id = :personId LIMIT 1")
    suspend fun getPPById(personId: Long): ProfilePictureStored?

    @Query("SELECT * FROM socials WHERE person_id = :id")
    suspend fun getSocialsByPerson(id: Long): List<SocialsEntity>

    // Socials
    @Query("SELECT * FROM socials WHERE person_id = :id")
    suspend fun getSocialsFromPerson(id: Long): List<SocialsEntity>
}