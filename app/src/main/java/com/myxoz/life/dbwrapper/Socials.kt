package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "socials", primaryKeys = ["person_id", "platform", "handle"])
data class SocialsEntity(
    @ColumnInfo(name = "person_id") val personId: Long,

    @ColumnInfo(name = "platform") val platform: Int,

    @ColumnInfo(name = "handle") val handle: String,
)

@Dao
interface SocialsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocial(social: SocialsEntity)

    @Query("SELECT * FROM socials WHERE person_id = :id")
    suspend fun getSocialsFromPerson(id: Long): List<SocialsEntity>

    @Query("DELETE FROM socials WHERE person_id = :id")
    suspend fun removeAllFromPerson(id: Long)
}
