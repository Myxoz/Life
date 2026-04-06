package com.myxoz.life.dbwrapper.events

import androidx.room.Dao
import androidx.room.Query

/** This special DAO has only read and not write access, this can therefore be shared outside of repos */
@Dao
interface ReadEventDetailsDao {
    // Events themselves
    @Query("SELECT * FROM events WHERE ends > :rangeStart AND start < :rangeEnd")
    suspend fun getEventsOverlapping(
        rangeStart: Long,
        rangeEnd: Long
    ): List<EventEntity>

    @Query("SELECT e.* FROM events e INNER JOIN people_mapping pm ON pm.event_id = e.id WHERE  pm.person_id = :personId")
    suspend fun getEventsWithPerson(personId: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE start > :after AND ends < :endsBefore AND (type = 6 OR type = 8)")
    suspend fun getAllPeopleEvents(after: Long, endsBefore: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE ends > :start AND :end > start")
    suspend fun getEventsBetween(start: Long, end: Long): List<EventEntity>

    // Also filter sleep events
    @Query("SELECT * FROM events WHERE start > :start AND type != 2 ORDER BY start ASC LIMIT 1")
    suspend fun getNonSleepEventAfter(start: Long): EventEntity?


    // Work
    @Query("SELECT * FROM work WHERE id = :id LIMIT 1")
    suspend fun getWork(id: Long): WorkEntity?


    // Social
    @Query("SELECT * FROM social WHERE id = :id LIMIT 1")
    suspend fun getSocial(id: Long): SocialEntity?


    // DigSoc
    @Query("SELECT * FROM digsoc WHERE id = :id LIMIT 1")
    suspend fun getDigSoc(id: Long): DigSocEntity?

        // DigSoc Mapping
        @Query("SELECT * FROM digsocmapping WHERE event_id = :id")
        suspend fun getDigSocMappingByEventId(id: Long): List<DigSocMappingEntity>


    // Learn Dao
    @Query("SELECT * FROM learn WHERE id = :id LIMIT 1")
    suspend fun getLearn(id: Long): LearnEntity?


    // Hobby
    @Query("SELECT * FROM hobby WHERE id = :id LIMIT 1")
    suspend fun getHobby(id: Long): HobbyEntiy?


    // Travel
    @Query("SELECT * FROM travel WHERE id = :id LIMIT 1")
    suspend fun getTavel(id: Long): TravelEntity?

        // Vehicle
        @Query("SELECT * FROM vehicle WHERE id = :id")
        suspend fun getVehicles(id: Long): List<VehicleEntity>


    // Spont
    @Query("SELECT * FROM spont WHERE id = :id LIMIT 1")
    suspend fun getSpont(id: Long): SpontEntity?

    // Timewaste
    @Query("SELECT * FROM timewaste WHERE id = :id LIMIT 1")
    suspend fun getTimewaste(id: Long): TimewasteEntity?

        // Timewaste Mapping
        @Query("SELECT * FROM timewasteplatform WHERE event_id = :id")
        suspend fun getTimewastePlatformsById(id: Long): List<TimewastePlatformEntity>

    // Additionals
    // Tags
    @Query("SELECT tag FROM tags WHERE id = :id")
    suspend fun getTagsByEventId(id: Long): List<Int>

    // People Mapping
    @Query("SELECT * FROM people_mapping AS p INNER JOIN events AS e WHERE e.id == p.event_id AND e.start > :start AND e.ends < :endsBefore AND (e.type = 8 OR e.type = 6)")
    suspend fun getAllPeopleMappingAfter(start: Long, endsBefore: Long): List<PeopleMappingEntity>

    @Query("SELECT * FROM people_mapping WHERE event_id = :id")
    suspend fun getPeopleMappingsByEventId(id: Long): List<PeopleMappingEntity>


    // Chunked
    @Query("SELECT * FROM tags WHERE id IN (:events)")
    suspend fun getAllTagsFor(events: List<Long>): List<TagsEntity>

    @Query("SELECT * FROM spont WHERE id IN (:events)")
    suspend fun getAllSpontFor(events: List<Long>): List<SpontEntity>

    @Query("SELECT * FROM hobby WHERE id IN (:events)")
    suspend fun getAllHobbyFor(events: List<Long>): List<HobbyEntiy>

    @Query("SELECT * FROM learn WHERE id IN (:events)")
    suspend fun getAllLearnFor(events: List<Long>): List<LearnEntity>

    @Query("SELECT * FROM social WHERE id IN (:events)")
    suspend fun getAllSocialFor(events: List<Long>): List<SocialEntity>

    @Query("SELECT * FROM travel WHERE id IN (:events)")
    suspend fun getAllTravelFor(events: List<Long>): List<TravelEntity>

    @Query("SELECT * FROM digsoc WHERE id IN (:events)")
    suspend fun getAllDigSocFor(events: List<Long>): List<DigSocEntity>

    @Query("SELECT * FROM work WHERE id IN (:events)")
    suspend fun getAllWorkFor(events: List<Long>): List<WorkEntity>

    @Query("SELECT * FROM timewaste WHERE id IN (:events)")
    suspend fun getAllTimewasteFor(events: List<Long>): List<TimewasteEntity>

    @Query("SELECT * FROM people_mapping WHERE event_id IN (:events)")
    suspend fun getAllPeopleFor(events: List<Long>): List<PeopleMappingEntity>

    @Query("SELECT * FROM vehicle WHERE id IN (:events)")
    suspend fun getAllVehiclesFor(events: List<Long>): List<VehicleEntity>

    @Query("SELECT * FROM digsocmapping WHERE event_id IN (:events)")
    suspend fun getAllDigSocMappingFor(events: List<Long>): List<DigSocMappingEntity>

    @Query("SELECT * FROM timewasteplatform WHERE event_id IN (:events)")
    suspend fun getAllTimewastePlatformsFor(events: List<Long>): List<TimewastePlatformEntity>
}