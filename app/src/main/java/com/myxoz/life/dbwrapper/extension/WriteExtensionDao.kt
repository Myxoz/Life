package com.myxoz.life.dbwrapper.extension

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WriteExtensionDao {
    @Query("DELETE FROM extension WHERE id = :id")
    suspend fun removeExtensionById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(extension: ExtensionEntity)
}