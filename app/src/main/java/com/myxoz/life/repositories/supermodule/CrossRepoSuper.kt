package com.myxoz.life.repositories.supermodule

import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.MutableStateFlow

class CrossRepoSuper(
    val repos: DatabaseInterface
) {
    private val getPersonFlowCache = StateFlowCache<Long, PersonSyncable?>{
        repos.peopleInterface.getPerson(it).subscribeToColdFlow(repos.appScope, null)
    }
    fun getPerson(personId: Long) = getPersonFlowCache.get(personId)

    private val getPeopleFlowCache = StateFlowCache<List<Long>, List<PersonSyncable>>{
        repos.peopleInterface.getPeople(it).subscribeToColdFlow(repos.appScope, listOf())
    }
    fun getPeople(personIds: List<Long>) = getPeopleFlowCache.get(personIds)


    private val getLocationByIdFLowCache = StateFlowCache<Long?, LocationSyncable?>{
        if(it == null || it == 0L) return@StateFlowCache MutableStateFlow(null)
        repos.locationInterface.getLocationById(it).subscribeToColdFlow(repos.appScope, null)
    }
    fun getLocationById(locationId: Long?) = getLocationByIdFLowCache.get(locationId)

    fun getCachedLocation(locationId: Long?) = (if(locationId != null) repos.locationInterface.getCachedLocation(locationId) else null)

    val getAllPeopleFlow = repos.peopleInterface.getAllPeople().subscribeToColdFlow(repos.appScope, listOf())
    val allLocations = repos.locationInterface.getAllLocations.subscribeToColdFlow(repos.appScope, listOf())
}