package com.myxoz.life.dbwrapper.locations

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myxoz.life.dbwrapper.LocationEntity

@Dao
interface WriteLocationsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("DELETE FROM location WHERE id = :id")
    suspend fun removeById(id: Long)
}