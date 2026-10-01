package com.myxoz.life.storage.interfaces

import com.myxoz.life.core.UnixWeek
import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.FullDaySyncable
import com.myxoz.life.storage.caches.PerformantCache.Companion.overwrite
import com.myxoz.life.storage.caches.RangedCache
import com.myxoz.life.storage.caches.RangedCache.Rangable.Companion.rangableTo
import com.myxoz.life.storage.convertion.SyncableFromDatabase.getFullDaySyncable
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.days.ReadDaysDao
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId

class DaySummaryInterface internal constructor(
    private val readDaysDao: ReadDaysDao,
    appScope: CoroutineScope,
    private val syncHelper: SyncHelper,
) {
    val zone: ZoneId = ZoneId.systemDefault()
    private val _cachedSummaries = RangedCache<LocalDate, Cached<FullDaySyncable>>(
        appScope,
        { getFullDaySyncable(it.toEpochDay(), readDaysDao).cached },
        { range ->
            val (from, to) = range.from.toEpochDay() to range.to.toEpochDay()
            val entries = readDaysDao.getDaysBetween(from, to)
            entries.map {
                it.fromEntity(readDaysDao).cached
            }
        },
        {
            LocalDate.ofEpochDay(
                it.value?.id ?: error("Database returned null (but not null) in DaySummaryRepo")
            )
        },
    )
    fun getDaySummary(date: LocalDate) = _cachedSummaries.flowByKey(date)
    fun getSummaryWeek(week: UnixWeek) = _cachedSummaries.flowByRange(week.start.rangableTo(week.end))

    suspend fun prefetchDay(from: LocalDate, to: LocalDate) = _cachedSummaries.fetchRange(from.rangableTo(to))
    internal fun updateCache(insert: FullDaySyncable){
        _cachedSummaries.overwrite(LocalDate.ofEpochDay(insert.id), insert)
    }
    suspend fun setAndStageDaySummary(insert: FullDaySyncable){
        insert.saveAndRequestSync(syncHelper)
        updateCache(insert)
    }
    fun getLastNDaysAsNonFlow(amount: Int) = List(amount) {
        LocalDate.now().minusDays(it + 1L)
    }.map { _cachedSummaries.getCached(it) }
}