package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.todos.ReadTodosDao
import com.myxoz.life.repositories.utils.VersionedDayedCache
import com.myxoz.life.repositories.utils.VersionedDayedCache.Companion.updateDayedCacheFromTo
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId

class TodoRepo(
    readTodosDao: ReadTodosDao,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val appScope: CoroutineScope,
    private val waitingSyncDao: WaitingSyncDao
) {
    val zone: ZoneId = ZoneId.systemDefault()
    private val _cache = VersionedDayedCache<Long, TodoSyncable?, TodoSyncable>(
        { id ->
            TodoSyncable.fromEntity(
                readTodosDao.getById(id) ?: return@VersionedDayedCache null
            )
        },
        { date, cache ->
            cache.overwriteAll(
                readTodosDao.getTodosBetween(
                    date.atStartAsMillis(zone),
                    date.atEndAsMillis(zone),
                ).map { it.id to TodoSyncable.fromEntity(it) }
            )
        },
    ){ cache, key, old, new ->
        cache.updateDayedCacheFromTo(
            old?.timestamp?.toLocalDate(zone),
            new?.timestamp?.toLocalDate(zone),
            new,
        ) { it.id == key }
    }
    fun getTodosForDay(date: LocalDate) = _cache.getDayFlowFor(appScope, date)
    fun getTodo(id: Long) = _cache.cache.flowByKey(appScope, id)
    suspend fun updateCachedTodo(new: TodoSyncable){
        _cache.cache.overwrite(new.id, new)
    }

    suspend fun deleteTodoFromServerDatabaseAndCache(syncable: TodoSyncable) {
        DeleteEntry.requestSyncDelete(waitingSyncDao, syncable)
        writeSyncableDaos.todosDao.removeTodoById(syncable.id)
        _cache.cache.overwrite(syncable.id, null)
    }

    suspend fun savePotentiallyUnsynced(potentiallyUnsynced: TodoSyncable) {
        val synced = potentiallyUnsynced.ensureSynced()
        _cache.cache.overwrite(synced.id, synced)
        synced.saveToDB(writeSyncableDaos)
        waitingSyncDao.requestSync(synced)
    }
}