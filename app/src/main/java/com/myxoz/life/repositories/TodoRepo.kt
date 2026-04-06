package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.todos.ReadTodosDao
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.repositories.utils.Cached.Companion.cached
import com.myxoz.life.repositories.utils.PerformantInterlockedCache
import com.myxoz.life.repositories.utils.PerformantInterlockedCache.Companion.remove
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId

class TodoRepo(
    private val readTodosDao: ReadTodosDao,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    appScope: CoroutineScope,
    private val waitingSyncDao: WaitingSyncDao
) {
    val zone: ZoneId = ZoneId.systemDefault()
    private val _cache = PerformantInterlockedCache.dayedCached<Long, TodoSyncable>(
        appScope,
        {
            listOfNotNull(it.value?.timestamp?.toLocalDate(zone))
        },
        { first, other -> first.value?.id == other.id },
        { id ->
            Cached.Value(
                TodoSyncable.fromEntity(
                    readTodosDao.getById(id) ?: return@dayedCached Cached.Null
                )
            )
        },
        { start, end ->
            readTodosDao.getTodosBetween(
                start.atStartAsMillis(zone),
                end.atEndAsMillis(zone),
            ).map { it.id to TodoSyncable.fromEntity(it).cached }
        },
    )
    fun getTodosForDay(date: LocalDate) = _cache.getInterlockedFlowFor(date)
    fun getTodo(id: Long) = _cache.flowFor( id)
    fun updateCachedTodo(new: TodoSyncable){
        _cache.overwrite(new.id, Cached.Value(new))
    }

    suspend fun deleteTodoFromServerDatabaseAndCache(syncable: TodoSyncable) {
        DeleteEntry.requestSyncDelete(waitingSyncDao, syncable)
        writeSyncableDaos.todosDao.removeTodoById(syncable.id)
        _cache.remove(syncable.id)
    }

    suspend fun savePotentiallyUnsynced(potentiallyUnsynced: TodoSyncable) {
        val synced = potentiallyUnsynced.ensureSynced()
        _cache.overwrite(synced.id, Cached.Value(synced))
        synced.saveToDB(writeSyncableDaos)
        waitingSyncDao.requestSync(synced)
    }
}