package com.myxoz.life.storage.interfaces

import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.caches.PerformantCache
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.locations.ReadLocationsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationInterface internal constructor(
    private val readLocationsDao: ReadLocationsDao,
    appScope: CoroutineScope,
    private val syncHelper: SyncHelper
) {
    private var fetchedAllLocations = false
    private val _cache = PerformantCache<Long, LocationSyncable>(appScope) {
        readLocationsDao.getLocationById(it)?.fromEntity()
            ?: throw Error("Location Syncable was fetched with an unknown id: $it. This is unsuported behavior. Check how you get the Location id.")
    }
    fun updateCached(location: LocationSyncable){
        _cache.overwrite(location.id, location)
    }
    suspend fun updateAndStage(location: LocationSyncable){
        location.saveAndRequestSync(syncHelper)
        updateCached(location)
    }
    fun getLocationById(id: Long) = _cache.flowByKey(id)
    suspend fun requestFetchAllLocations(){
        if(fetchedAllLocations) return
        fetchedAllLocations = true
        _cache.overwriteAll(
            readLocationsDao.getAllLocations().map { it.id to it.fromEntity() }
        )
    }
    val getAllLocations = _cache.allValuesFlow
    suspend fun queryByCoordinate(targetLat: Double, targetLon: Double, allLocations: List<LocationSyncable>? = null): LocationSyncable? {
        val allLocations = if(allLocations != null) allLocations else {
            requestFetchAllLocations()
            _cache.allValuesFlow.first()
        }

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
    fun getCachedLocation(locationid: Long) = _cache.getCached(locationid)
    init {
        appScope.launch {
            _cache.overwriteAll(
                readLocationsDao.getAllLocations().map {
                    it.id to it.fromEntity()
                }
            )
            fetchedAllLocations = true
        }
    }
}