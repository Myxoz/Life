package com.myxoz.life.repositories

import android.content.Context
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.repositories.utils.PerformantCache
import com.myxoz.life.repositories.utils.PerformantInterlockedCache
import com.myxoz.life.screens.options.settings.ME_ID
import com.myxoz.life.utils.daysUntil
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
    private val _cachedPeople = PerformantInterlockedCache.dayedSame(
        appScope,
        {
            it.birthday?.let { birthday -> LocalDate.ofEpochDay(birthday).allFutureBirthdays() } ?: emptyList()
        },
        { first, other -> first.id == other.id },
        { id ->
            PersonSyncable.from(
                readPeopleDao, readPeopleDao.getPersonById(id).let { entity ->
                    if(entity == null)
                        throw Error("Tried to get PersonSyncable for an id, not present in db. This behaviour is unsupported, please review getId-Code. Tried to fetch: $id")
                    entity
                }
            )
        },
        { from, to ->
            from.daysUntil(to).flatMap { date ->
                readSyncableDaos.peopleDao.getPeopleWithBirthdayAt(date).map { entry ->
                    entry.id to PersonSyncable.from(readPeopleDao, entry)
                }
            }
        }
    ) { _, new ->
        if(new.id == ME_ID) _meFlow.update { new }
    }
    fun getPerson(personId: Long) = _cachedPeople.flowFor(personId)
    fun getPeople(personIds: List<Long>) = _cachedPeople.flowsFor(personIds)
    suspend fun getCurrentPersonNotAsFlow(personId: Long) = _cachedPeople.getContent(personId)
    private val _pps = PerformantCache<Long, ProfilePictureSyncable>(appScope) {
        ProfilePictureSyncable.getSyncable(it, context, readPeopleDao)
    }

    suspend fun updatePP(new: ProfilePictureSyncable) = _pps.overwrite(new.id, new)
    suspend fun updatePP(personId: Long, base64: String?) {
        val new = ProfilePictureSyncable(personId, base64)
        new.saveToDB(writeSyncableDaos)
        _pps.overwrite(personId, new)
        new.addToWaitingSyncDao(waitingSyncDao)
    }
    fun getProfilePicture(id: Long) = _pps.flowByKey(id)
    fun updateCacheOnly(person: PersonSyncable) {
        _cachedPeople.overwrite(person.id, person)
    }
    suspend fun updateAndStageSync(person: PersonSyncable) {
        person.updateAndStageSync(writeSyncableDaos, waitingSyncDao)
        updateCacheOnly(person)
    }
    fun getPeopleWithIbanLike(iban: String): Flow<List<PersonSyncable>> {
        requireAllPeople()
        return _cachedPeople.allValuesFlow.map { people ->
            people.filter { it.iban == iban }
        }
    }
    fun getAllPeople(): Flow<List<PersonSyncable>> {
        requireAllPeople()
        return _cachedPeople.allValuesFlow
    }
    fun getPeopleWithBirthdayAt(date: LocalDate) = _cachedPeople.getInterlockedFlowFor(date)
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
            // We mark this before loading to avoid race conditions
            _cachedPeople.markAllEntriesAsLoaded()

            _cachedPeople.overwriteAll(
                readPeopleDao.getAllPeople().map {
                    it.id to PersonSyncable.from(readPeopleDao,it)
                }
            )
        }
    }
    fun getCachedPeopleById(ids: List<Long>) = ids.mapNotNull { _cachedPeople.getCachedContent(it) }
}