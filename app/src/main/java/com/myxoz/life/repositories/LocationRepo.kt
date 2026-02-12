package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.repositories.utils.VersionedCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationRepo(
    private val readSyncableDaos: API.ReadSyncableDaos,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val waitingSyncDao: WaitingSyncDao,
    private val appScope: CoroutineScope
) {
    val readLocationsDao = readSyncableDaos.locationsDao
    var fetchedAllLocations = false
    private val _cache = VersionedCache<Long, LocationSyncable>(
        {
            LocationSyncable.from(
                readLocationsDao.getLocationById(it).let { locationEntity ->
                    if(locationEntity == null)
                        throw Error("Location Syncable was fetched with an unknown id: $it. This is unsuported behavior. Check how you get the Location id.")
                    locationEntity
                }
            )
        }
    )
    suspend fun update(location: LocationSyncable){
        _cache.overwrite(location.id, location)
    }
    suspend fun updateAndStage(location: LocationSyncable){
        location.saveToDB(writeSyncableDaos)
        update(location)
        location.addToWaitingSyncDao(waitingSyncDao)
    }
    fun getLocationById(id: Long) = _cache.flowByKey(appScope, id)
    suspend fun requestFetchAllLocations(){
        if(fetchedAllLocations) return
        fetchedAllLocations = true
        _cache.overwriteAll(
            readSyncableDaos.locationsDao.getAllLocations().map { it.id to LocationSyncable.from(it) }
        )
    }
    val getAllLocations = _cache.allValuesFlow
    suspend fun queryByCoordinate(targetLat: Double, targetLon: Double): LocationSyncable? {
        requestFetchAllLocations()
        val allLocations = _cache.allValuesFlow.first().map { it.data }

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
    fun getCachedLocation(locationid: Long) = _cache.getCached(locationid)?.data
    init {
        appScope.launch {
            _cache.overwriteAll(
                readLocationsDao.getAllLocations().map {
                    it.id to LocationSyncable.from(it)
                }
            )
        }
    }
}