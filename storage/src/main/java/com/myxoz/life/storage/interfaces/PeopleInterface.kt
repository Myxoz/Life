package com.myxoz.life.storage.interfaces

import android.content.Context
import android.graphics.Bitmap
import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.PersonSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable
import com.myxoz.life.storage.api.syncables.ProfilePictureSyncable.Companion.loadBase64ByPerson
import com.myxoz.life.storage.caches.InterlockedCache
import com.myxoz.life.storage.caches.PerformantCache
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.people.PersonEntity
import com.myxoz.life.storage.dbwrapper.people.ReadPeopleDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.MonthDay

class PeopleInterface internal constructor(
    private val readPeopleDao: ReadPeopleDao,
    private val context: Context,
    private val appScope: CoroutineScope,
    private val syncHelper: SyncHelper,
) {
    private var requestedAllPeople = false
    private val _meFlow = MutableStateFlow<PersonSyncable?>(null)
    val meFlow: StateFlow<PersonSyncable?> = _meFlow
    private val _cachedPeople = InterlockedCache.dayedSame(
        appScope,
        { first, other -> first.id == other.id },
        {
            it.birthday?.allFutureBirthdays() ?: emptyList()
        },
        { it.id },
        { id ->
            readPeopleDao.getPersonById(id)
                .let { entity ->
                    if(entity == null && id != ME_ID) throw Error("Tried to get PersonSyncable for an id, not present in db. This behaviour is unsupported, please review getId-Code. Tried to fetch: $id")
                    entity ?: PersonEntity(ME_ID, "Ich", null, null, null, null, null)
                }
                .fromEntity(readPeopleDao)
        },
        { range ->
            range.between.flatMap { date ->
                readPeopleDao.getPeopleWithBirthdayAt(date).map { entry ->
                    entry.fromEntity(readPeopleDao)
                }
            }
        }
    ) { _, new ->
        if(new.id == ME_ID) _meFlow.update { new }
    }
    fun getPerson(personId: Long) = _cachedPeople.flowFor(personId)
    fun getPeople(personIds: Set<Long>) = _cachedPeople.flowsFor(personIds.toList())
    private val _pps = PerformantCache<Long, ProfilePictureSyncable>(appScope) { id ->
        val pp = readPeopleDao.getPPById(id)
        ProfilePictureSyncable(id, if(pp != null && pp.hasPP) loadBase64ByPerson(context, id) else null)
    }

    fun updatePP(new: ProfilePictureSyncable) = _pps.overwrite(new.id, new)
    suspend fun updatePP(personId: Long, newProfilePicture: Bitmap) {
        val new = ProfilePictureSyncable(personId, ProfilePictureSyncable.bitmapToBase64(newProfilePicture))
        new.saveAndRequestSync(syncHelper)
        _pps.overwrite(personId, new)
    }
    fun getProfilePicture(id: Long) = _pps.flowByKey(id)
    fun updateCache(person: PersonSyncable) {
        _cachedPeople.overwrite(person.id, person)
    }
    suspend fun updateAndStageSync(person: PersonSyncable) {
        person.saveAndRequestSync(syncHelper)
        updateCache(person)
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
        // Lets hope the life expectancy doesnt rise more than this /halfjoke
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
        // We mark this before loading to avoid race conditions
        _cachedPeople.markAllEntriesAsLoaded()
        appScope.launch {
            _cachedPeople.overwriteAll(
                readPeopleDao.getAllPeople().map {
                    it.id to it.fromEntity(readPeopleDao)
                }
            )
        }
    }
    fun getCachedPeopleById(ids: List<Long>) = ids.mapNotNull { _cachedPeople.getCachedContent(it) }
    companion object {
        const val ME_ID = 1L
    }
}