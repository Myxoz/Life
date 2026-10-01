package com.myxoz.life.storage.dbwrapper.extension

import androidx.room.Dao
import androidx.room.Query

@Dao
internal interface ReadExtensionDao {
    @Query("SELECT * FROM extension WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExtensionEntity?
}