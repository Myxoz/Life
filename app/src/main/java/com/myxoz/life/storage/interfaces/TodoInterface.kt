package com.myxoz.life.storage.interfaces

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.DeleteEntry
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao
import com.myxoz.life.storage.dbwrapper.todos.ReadTodosDao
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.PerformantInterlockedCache
import com.myxoz.life.storage.interfaces.utils.PerformantInterlockedCache.Companion.remove
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId

class TodoInterface(
    private val readTodosDao: ReadTodosDao,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    appScope: CoroutineScope,
    private val waitingSyncDao: WaitingSyncDao
) {
    val zone: ZoneId = ZoneId.systemDefault()
    private val _cache = PerformantInterlockedCache.dayedCached(
        appScope,
        { first, other -> first.value?.id == other.id },
        {
            listOfNotNull(it.value?.timestamp?.toLocalDate(zone))
        },
        { it.id },
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
            ).map { TodoSyncable.fromEntity(it) }
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