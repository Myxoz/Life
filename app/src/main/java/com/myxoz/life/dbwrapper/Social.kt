package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "social")
data class SocialEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,

    @ColumnInfo(name = "more") val more: Boolean,
)

@Dao
interface SocialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(socialEntity: SocialEntity)

    @Query("SELECT * FROM social WHERE id = :id LIMIT 1")
    suspend fun getEvent(id: Long): SocialEntity?

    @Query("DELETE FROM social WHERE id = :id")
    suspend fun removeById(id: Long)
}
