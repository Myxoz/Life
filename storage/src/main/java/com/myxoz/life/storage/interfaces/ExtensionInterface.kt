package com.myxoz.life.storage.interfaces

import com.myxoz.life.storage.api.helper.SyncHelper
import com.myxoz.life.storage.api.helper.SyncHelper.Companion.saveAndRequestSync
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.caches.PerformantCache
import com.myxoz.life.storage.caches.PerformantCache.Companion.overwrite
import com.myxoz.life.storage.dbwrapper.extension.ReadExtensionDao
import com.myxoz.life.storage.interfaces.utils.Cached
import com.myxoz.life.storage.interfaces.utils.Cached.Companion.cached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExtensionInterface internal constructor(
    appScope: CoroutineScope,
    private val readExtensionDao: ReadExtensionDao,
    private val syncHelper: SyncHelper
) {
    internal fun <T: ExtensionSyncable.Extendable> overwriteCache(
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
        ExtensionSyncable(type, content).saveAndRequestSync(syncHelper)
    }

    private val _cache = PerformantCache(appScope) { key: Long ->
        val json = readExtensionDao.getById(key) ?: return@PerformantCache Cached.Null
        val type = ExtensionSyncable.ExtensionSyncableType.typeFromId(key)
        type?.parser?.fromString(json.data).cached
    }
    @PublishedApi
    internal fun <T: ExtensionSyncable.Extendable> rawFlowFor(type: ExtensionSyncable.ExtensionSyncableType<T>): Flow<Cached<ExtensionSyncable.Extendable>?> {
        return _cache.flowByKey(type.id)
    }
    @PublishedApi
    internal suspend fun getRawValue(id: Long) = _cache.getValue(id)
    companion object {
        inline fun <reified T: ExtensionSyncable.Extendable> ExtensionInterface.flowFor(type: ExtensionSyncable.ExtensionSyncableType<T>): Flow<T?> {
            return rawFlowFor(type).map { if(it?.value != null) it.value as? T else null }
        }
        suspend inline fun <reified T : ExtensionSyncable.Extendable> ExtensionInterface.getExtension(
            type: ExtensionSyncable.ExtensionSyncableType<T>,
        ): T? {
            return getRawValue(type.id).value as? T
        }
        suspend inline fun <reified T : ExtensionSyncable.Extendable> ExtensionInterface.updateAndSyncWith(
            type: ExtensionSyncable.ExtensionSyncableType<T>,
            update: (Cached<T>) -> T
        ) {
            val current = when(val currentRaw = getRawValue(type.id)) {
                is Cached.Value -> {
                    val casted = currentRaw.content as? T ?: return
                    Cached.Value(casted)
                }
                Cached.Null -> Cached.Null
            }

            saveAndSync(type, update(current))
        }
    }
}