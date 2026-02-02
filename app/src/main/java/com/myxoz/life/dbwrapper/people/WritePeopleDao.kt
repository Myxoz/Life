package com.myxoz.life.dbwrapper.people

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WritePeopleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(people: List<PersonEntity>)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Query("DELETE FROM profilepicture WHERE person_id = :personId")
    suspend fun deleteProfilePicture(personId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfilePicture(entry: ProfilePictureStored)

    // Social Platforms
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocial(social: SocialsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSocials(social: List<SocialsEntity>)

    @Query("DELETE FROM socials WHERE person_id = :id")
    suspend fun removeAllSocialsFor(id: Long)

}