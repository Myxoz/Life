package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Entity(tableName = "location", primaryKeys = ["id"])
data class LocationEntity(
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "longitude") val longitude: Double,

    @ColumnInfo(name = "lat") val lat: Double,

    @ColumnInfo(name = "radiusM") val radiusM: Int,

    @ColumnInfo(name = "ssid") val ssid: String?,

    @ColumnInfo(name = "street") val street: String?,

    @ColumnInfo(name = "number") val number: String?,

    @ColumnInfo(name = "city") val city: String?,

    @ColumnInfo(name = "country") val country: String,
)

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Query("SELECT * FROM location WHERE id = :id LIMIT 1")
    suspend fun getLocation(id: Long): LocationEntity?


    @Query("SELECT * FROM location WHERE street = :street AND number = :number AND city = :city LIMIT 1")
    suspend fun queryLocation(street: String, number: String, city: String): LocationEntity?

    @Query("SELECT * FROM location")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("DELETE FROM location WHERE id = :id")
    suspend fun removeById(id: Long)
}
