package com.myxoz.life.dbwrapper.events

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WriteEventDetailsDao {
    // Events themself
    @Query("DELETE FROM events WHERE id = :id")
    suspend fun removeEventById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    // Work
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: WorkEntity)

    @Query("DELETE FROM work WHERE id = :id")
    suspend fun removeWork(id: Long)

    // Social
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocial(socialEntity: SocialEntity)

    @Query("DELETE FROM social WHERE id = :id")
    suspend fun removeSocial(id: Long)

    // DigSoc
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDicSoc(event: DigSocEntity)

    @Query("DELETE FROM digsoc WHERE id = :id")
    suspend fun removeDigSoc(id: Long)

    // Learn
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearn(learnEntity: LearnEntity)

    @Query("DELETE FROM learn WHERE id = :id")
    suspend fun removeLearn(id: Long)

    // Hobby
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHobby(hobby: HobbyEntiy)

    @Query("DELETE FROM hobby WHERE id = :id")
    suspend fun removeHobby(id: Long)

    // Tavel
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: TravelEntity)

    @Query("DELETE FROM travel WHERE id = :id")
    suspend fun removeTravel(id: Long)

    // Spont
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpont(spont: SpontEntity)

    @Query("DELETE FROM spont WHERE id = :id")
    suspend fun removeSpont(id: Long)

    // Additionals
    // Tags
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagsEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun removeTags(id: Long)

    // DigSocMapping
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDigSocMapping(mapping: DigSocMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDigSocMappings(mapping: List<DigSocMappingEntity>)

    @Query("DELETE FROM digsocmapping WHERE event_id = :id")
    suspend fun deleteDigSocMapping(id: Long)

    // People Mapping
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeopleMapping(person: PeopleMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPeopleMappings(people: List<PeopleMappingEntity>)

    @Query("DELETE FROM people_mapping WHERE event_id = :id")
    suspend fun deletePeopleMapping(id: Long)

    // Vehicle
    @Query("DELETE FROM vehicle WHERE id = :id")
    suspend fun removeVehicleById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: VehicleEntity)
}