package com.myxoz.life.dbwrapper.locations

import androidx.room.Dao
import androidx.room.Query
import com.myxoz.life.dbwrapper.LocationEntity

@Dao
interface ReadLocationsDao {
    @Query("SELECT * FROM location WHERE id = :id LIMIT 1")
    suspend fun getLocationById(id: Long): LocationEntity?

    @Query("SELECT * FROM location WHERE id IN (:ids)")
    suspend fun getLocationsByIds(ids: List<Long>): List<LocationEntity>

    @Query("SELECT * FROM location WHERE street = :street AND number = :number AND city = :city LIMIT 1")
    suspend fun queryLocation(street: String, number: String, city: String): LocationEntity?

    @Query("SELECT * FROM location")
    suspend fun getAllLocations(): List<LocationEntity>
}