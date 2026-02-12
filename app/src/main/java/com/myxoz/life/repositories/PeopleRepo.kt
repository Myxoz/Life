package com.myxoz.life.repositories

import android.content.Context
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.repositories.utils.VersionedCache
import com.myxoz.life.repositories.utils.VersionedDayedCache
import com.myxoz.life.screens.options.ME_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.MonthDay

class PeopleRepo(
    val readSyncableDaos: API.ReadSyncableDaos,
    val writeSyncableDaos: API.WriteSyncableDaos,
    val waitingSyncDao: WaitingSyncDao,
    val context: Context,
    val appScope: CoroutineScope
) {
    private var requestedAllPeople = false
    private val readPeopleDao = readSyncableDaos.peopleDao
    private val _meFlow = MutableStateFlow<PersonSyncable?>(null)
    val meFlow: StateFlow<PersonSyncable?> = _meFlow
    private val _cachedPeople = VersionedDayedCache(
        { id ->
            PersonSyncable.from(
                readPeopleDao, readPeopleDao.getPersonById(id).let { entity ->
                    if(entity == null)
                        throw Error("Tried to get PersonSyncable for an id, not present in db. This behaviour is unsupported, please review getId-Code. Tried to fetch: $id")
                    entity
                }
            )
        },
        { date, cache ->
            val allBirthdayed = readSyncableDaos.peopleDao.getPeopleWithBirthdayAt(date).map { entry ->
                PersonSyncable.from(readPeopleDao, entry)
            }
            cache.overwriteAll(allBirthdayed.map { it.id to it })
        }
    ) { cache, _, old, new ->
        suspend fun updateBirthday(epochDay: Long, add: Boolean) {
            cache.updateKeysWith(LocalDate.ofEpochDay(epochDay).allFutureBirthdays()) {
                if(add) it + new else it.filterNot { person -> person.id == new.id }
            }
        }
        if(new.id == ME_ID) _meFlow.update { new }
        if(old?.birthday == null && new.birthday != null) { // We added birthday
            updateBirthday(new.birthday, true)
        } else if(old?.birthday != null && new.birthday != null && old.birthday != new.birthday) { // We updated birthday
            updateBirthday(old.birthday, false)
            updateBirthday(new.birthday, true)
        } else if(old?.birthday != null && new.birthday == null) { // We removed birthday
            updateBirthday(old.birthday, false)
        }
    }
    fun getPerson(personId: Long) = _cachedPeople.cache.flowByKey(appScope, personId)
    fun getPeople(personIds: List<Long>) = _cachedPeople.cache.flowByKeys(appScope, personIds)
    suspend fun getCurrentPersonNotAsFlow(personId: Long) = _cachedPeople.cache.get(personId)
    private val _pps = VersionedCache<Long, ProfilePictureSyncable>(
        {
            ProfilePictureSyncable.getSyncable(it, context, readPeopleDao)
        }
    )
    suspend fun updatePP(new: ProfilePictureSyncable) = _pps.overwrite(new.id, new)
    suspend fun updatePP(personId: Long, base64: String?) {
        val new = ProfilePictureSyncable(personId, base64)
        new.saveToDB(writeSyncableDaos)
        _pps.overwrite(personId, new)
        new.addToWaitingSyncDao(waitingSyncDao)
    }
    fun getProfilePicture(id: Long) = _pps.flowByKey(appScope, id)
    suspend fun updateCacheOnly(person: PersonSyncable) {
        _cachedPeople.cache.overwrite(person.id, person)
    }
    suspend fun updateAndStageSync(person: PersonSyncable) {
        person.updateAndStageSync(writeSyncableDaos, waitingSyncDao)
        updateCacheOnly(person)
    }
    fun getPeopleWithIbanLike(iban: String): Flow<List<PersonSyncable>> {
        requireAllPeople()
        return _cachedPeople.cache.allValuesFlow.map { people ->
            people.filter { it.data.iban == iban }.map { it.data }
        }
    }
    fun getAllPeople(): Flow<List<PersonSyncable>> {
        requireAllPeople()
        return _cachedPeople.cache.allMapedFlows.map { it.values.toList() }
    }
    fun getPeopleWithBirthdayAt(date: LocalDate) = _cachedPeople.getDayFlowFor(appScope, date).map { it?.data }
    fun LocalDate.allFutureBirthdays(maxAge: Int = 130): List<LocalDate> {
        // Lets hope the life expectancy doesnt raise more than this /halfjoke
        val monthDay = MonthDay.from(this)
        val startYear = this.year
        return (1..maxAge).mapNotNull { offset ->
                val year = startYear + offset
                monthDay.atYear(year).takeIf { it.monthValue == monthDay.monthValue }
            }
    }
    fun requireAllPeople(){
        if(requestedAllPeople) return
        requestedAllPeople = true
        appScope.launch {
            _cachedPeople.cache.overwriteAll(
                readPeopleDao.getAllPeople().map {
                    it.id to PersonSyncable.from(readPeopleDao,it)
                }
            )
            _cachedPeople.markAllDaysAsLoaded()
        }
    }
    fun getCachedPeopleById(ids: List<Long>) = ids.mapNotNull { _cachedPeople.cache.getCached(it)?.data }
}