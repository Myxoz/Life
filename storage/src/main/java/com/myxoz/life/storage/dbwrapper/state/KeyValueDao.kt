package com.myxoz.life.storage.dbwrapper.state

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface KeyValueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KeyValueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entity: List<KeyValueEntity>)

    @Delete
    suspend fun delete(entity: KeyValueEntity)

    @Query("DELETE FROM keyvalue WHERE `key` = :key")
    suspend fun delete(key: String)
    @Query("SELECT * FROM keyvalue")
    fun getAll(): List<KeyValueEntity>
}