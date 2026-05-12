package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.repositories.utils.Cached.Companion.cached
import com.myxoz.life.repositories.utils.PerformantCache.Companion.overwrite
import com.myxoz.life.repositories.utils.RangedPerformantCache
import com.myxoz.life.utils.UnixWeek
import com.myxoz.life.utils.datesThrough
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
    private val _cachedSummaries = RangedPerformantCache<LocalDate, Cached<FullDaySyncable>>(
        appScope,
        { FullDaySyncable.fromDB(readSyncablesDaos, it.toEpochDay()).cached },
        { from, to -> from.datesThrough(to) },
        { from, to ->
            val between = from.datesThrough(to)
            val entries =
                readSyncablesDaos.daysDao.getDaysBetween(from.toEpochDay(), to.toEpochDay())
            val screenTime =
                readSyncablesDaos.daysDao.getScreenTimesByRange(from.toEpochDay(), to.toEpochDay())
                    .groupBy { it.date }
            entries.map {
                FullDaySyncable.fromEntity(
                    it.id,
                    it,
                    screenTime[it.id] ?: emptyList()
                ).cached
            }
        },
        { LocalDate.ofEpochDay(it.value?.id ?: error("Database returned null (but not null) in DaySummaryRepo")) },
    )
    fun getDaySummary(date: LocalDate) = _cachedSummaries.flowByKey(date)
    fun getSummaryWeek(week: UnixWeek) = _cachedSummaries.flowByRange(week.start, week.end)

    suspend fun prefetchDay(from: LocalDate, to: LocalDate) = _cachedSummaries.fetchRange(from, to)
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