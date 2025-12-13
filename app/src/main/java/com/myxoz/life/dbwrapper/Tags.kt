package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "tags", primaryKeys = ["id", "tag"])
data class TagsEntity(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "tag") val tag: Int,
)

@Dao
interface TagsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagsEntity)

    @Query("SELECT tag FROM tags WHERE id = :id")
    suspend fun getTagsByEventId(id: Long): List<Int>

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun removeById(id: Long)
}
