package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.repositories.utils.VersionedCache
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
    private val _cachedSummaries = VersionedCache<LocalDate, FullDaySyncable?>(
        {
            FullDaySyncable.fromDB(readSyncablesDaos, it.toEpochDay())
        }
    )
    fun getDaySummary(date: LocalDate) = _cachedSummaries.flowByKey(appScope, date)

    fun prefetchDay(date: LocalDate){
        _cachedSummaries.prepare(appScope, date)
    }
    suspend fun setDaySummary(insert: FullDaySyncable){
        _cachedSummaries.overwrite(LocalDate.ofEpochDay(insert.id), insert)
    }
    suspend fun setAndStageDaySummary(insert: FullDaySyncable){
        insert.addToWaitingSyncDao(waitingSyncDao)
        insert.saveToDB(writeSyncableDaos)
        _cachedSummaries.overwrite(LocalDate.ofEpochDay(insert.id), insert)
    }
    suspend fun getLastNDaysAsNonFlow(amount: Int) = List(amount) {
        LocalDate.now().minusDays(it + 1L)
    }.map { _cachedSummaries.get(it).data }
}