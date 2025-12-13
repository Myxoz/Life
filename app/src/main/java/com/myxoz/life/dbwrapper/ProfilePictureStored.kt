package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "profilepicture")
data class ProfilePictureStored(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "person_id") val personId: Long,

    @ColumnInfo(name = "has_pp") val hasPP: Boolean,
)

@Dao
interface ProfilePictureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfilePicture(entry: ProfilePictureStored)

    @Query("SELECT * FROM profilepicture WHERE person_id = :personId LIMIT 1")
    suspend fun getPPById(personId: Long): ProfilePictureStored?

    @Query("DELETE FROM profilepicture WHERE person_id = :personId")
    suspend fun deleteDay(personId: Long)
}
