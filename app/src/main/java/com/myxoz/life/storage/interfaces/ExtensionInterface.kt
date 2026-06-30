package com.myxoz.life.storage.interfaces

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.dbwrapper.WaitingSyncDao
import com.myxoz.life.storage.dbwrapper.extension.ReadExtensionDao
import com.myxoz.life.storage.dbwrapper.extension.WriteExtensionDao
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import com.myxoz.life.storage.interfaces.utils.PerformantCache
import com.myxoz.life.storage.interfaces.utils.PerformantCache.Companion.overwrite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExtensionInterface(
    private val appScope: CoroutineScope,
    private val readExtensionDao: ReadExtensionDao,
    private val writeExtensionDao: WriteExtensionDao,
    private val writeSyncableDaos: API.WriteSyncableDaos,
    private val waitingSync: WaitingSyncDao
) {
    fun <T: ExtensionSyncable.Extendable> overwriteCache(
        type: ExtensionSyncable.ExtensionSyncableType<T>,
        content: T
    ) {
        _cache.overwrite(type.id, content)
    }

    suspend fun <T: ExtensionSyncable.Extendable> saveAndSync(
        type: ExtensionSyncable.ExtensionSyncableType<T>,
        content: T
    ) {
        overwriteCache(type, content)
        val syncable = ExtensionSyncable(type, content)
        syncable.saveToDB(writeSyncableDaos)
        waitingSync.requestSync(syncable)
    }

    val _cache = PerformantCache(appScope) { key: Long ->
        val json = readExtensionDao.getById(key) ?: return@PerformantCache Cached.Null
        val type = ExtensionSyncable.ExtensionSyncableType.typeFromId(key)
        type?.parser?.fromString(json.data).cached
    }
    inline fun <reified T: ExtensionSyncable.Extendable> flowFor(type: ExtensionSyncable.ExtensionSyncableType<T>): Flow<T?> {
        return _cache
            .flowByKey(type.id)
            .map { if(it?.value != null) it.value as? T else null }
    }
    suspend inline fun <reified T : ExtensionSyncable.Extendable> updateAndSyncWith(
        type: ExtensionSyncable.ExtensionSyncableType<T>,
        update: (Cached<T>) -> T
    ) {
        val current = when (val currentRaw = _cache.getValue(type.id)) {
            is Cached.Value -> {
                val casted = currentRaw.content as? T ?: return
                Cached.Value(casted)
            }
            Cached.Null -> Cached.Null
        }

        saveAndSync(type, update(current))
    }

    suspend inline fun <reified T : ExtensionSyncable.Extendable> getExtension(
        type: ExtensionSyncable.ExtensionSyncableType<T>,
    ): T? {
        return _cache.getValue(type.id).value as? T
    }
}