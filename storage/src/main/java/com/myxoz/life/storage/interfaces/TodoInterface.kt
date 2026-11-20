package com.myxoz.life.storage.interfaces

import com.myxoz.life.core.Rangable.Companion.startAndEndInMs
import com.myxoz.life.core.toLocalDate
import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.deleteAndSync
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.caches.InterlockedCache
import com.myxoz.life.storage.caches.InterlockedCache.Companion.remove
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.todos.ReadTodosDao
import com.myxoz.life.storage.interfaces.utils.Cached
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDate
import java.time.ZoneId

class TodoInterface internal constructor(
    private val readTodosDao: ReadTodosDao,
    appScope: CoroutineScope,
    private val syncHelper: SyncHelper
) {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val _cache = InterlockedCache.dayedCached(
        appScope,
        { first, other -> first.value?.id == other.id },
        {
            listOfNotNull(it.value?.timestamp?.toLocalDate(zone))
        },
        { it.id },
        { id ->
            Cached.by(
                readTodosDao.getById(id)?.fromEntity()
            )
        },
        { range ->
            val (start, end) = range.startAndEndInMs(zone)
            readTodosDao.getTodosBetween(start, end).map { it.fromEntity() }
        },
    )
    fun getTodosForDay(date: LocalDate) = _cache.getInterlockedFlowFor(date)
    fun getTodo(id: Long) = _cache.flowFor( id)
    internal fun updateCachedTodo(new: TodoSyncable){
        _cache.overwrite(new.id, Cached.Value(new))
    }

    suspend fun delete(syncable: TodoSyncable) {
        syncable.deleteAndSync(syncHelper)
        _cache.remove(syncable.id)
    }

    suspend fun save(synced: TodoSyncable) {
        synced.saveAndRequestSync(syncHelper)
        updateCachedTodo(synced)
    }
}