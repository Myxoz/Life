package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "hobby")
data class HobbyEntiy(
    @PrimaryKey(false)
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "title") val title: String,

    @ColumnInfo(name = "details") val details: String?,
)

@Dao
interface HobbyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHobby(hobby: HobbyEntiy)

    @Query("SELECT * FROM hobby WHERE id = :id LIMIT 1")
    suspend fun getHobby(id: Long): HobbyEntiy?

    @Query("DELETE FROM hobby WHERE id = :id")
    suspend fun removeById(id: Long)
}
