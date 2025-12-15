package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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

    suspend fun queryByCoordinate(targetLat: Double, targetLon: Double): LocationEntity? {
        val allLocations = this.getAllLocations()

        return allLocations
            .mapNotNull { entity ->
                val distance = calculateHaversineDistance(
                    targetLat, targetLon,
                    entity.lat, entity.longitude
                )
                if (distance <= entity.radiusM) entity to distance else null
            }
            .minByOrNull { it.second }
            ?.first
    }

    // Haversine formula to calculate distance in meters [web:6][web:12]
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Radius of Earth in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return R * c
    }

    @Query("DELETE FROM location WHERE id = :id")
    suspend fun removeById(id: Long)
}
