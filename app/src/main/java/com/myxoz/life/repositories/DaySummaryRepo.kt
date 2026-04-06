package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.repositories.utils.Cached.Companion.cached
import com.myxoz.life.repositories.utils.PerformantCache
import com.myxoz.life.repositories.utils.PerformantCache.Companion.overwrite
import com.myxoz.life.utils.daysUntil
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId

class DaySummaryRepo(
    val readSyncablesDaos: API.ReadSyncableDaos,
    val writeSyncableDaos: API.WriteSyncableDaos,
    val waitingSyncDao: WaitingSyncDao,
    val appScope: CoroutineScope
) {
    val zone: ZoneId = ZoneId.systemDefault()
    private val _cachedSummaries = PerformantCache<LocalDate, Cached<FullDaySyncable>>(appScope) {
        FullDaySyncable.fromDB(readSyncablesDaos, it.toEpochDay()).cached
    }
    fun getDaySummary(date: LocalDate) = _cachedSummaries.flowByKey(date)

    suspend fun prefetchDay(from: LocalDate, to: LocalDate) {
        val between = from.daysUntil(to)
        if(!between.all { _cachedSummaries.hasCached(it) }) return
        val entries = readSyncablesDaos.daysDao.getDaysBetween(from.toEpochDay(), to.toEpochDay())
        val screenTime = readSyncablesDaos.daysDao.getScreenTimesByRange(from.toEpochDay(), to.toEpochDay()).groupBy { it.date }
        _cachedSummaries.overwriteAll(
            entries.map { it.id.toLocalDate(zone) to FullDaySyncable.fromEntity(
                it.id,
                it,
                screenTime[it.id] ?: emptyList()
            ).cached }
        )
    }
    fun setDaySummary(insert: FullDaySyncable){
        _cachedSummaries.overwrite(LocalDate.ofEpochDay(insert.id), insert)
    }
    suspend fun setAndStageDaySummary(insert: FullDaySyncable){
        insert.addToWaitingSyncDao(waitingSyncDao)
        insert.saveToDB(writeSyncableDaos)
        _cachedSummaries.overwrite(LocalDate.ofEpochDay(insert.id), insert)
    }
    fun getLastNDaysAsNonFlow(amount: Int) = List(amount) {
        LocalDate.now().minusDays(it + 1L)
    }.map { _cachedSummaries.getCached(it) }
}