package com.myxoz.life.storage.dbwrapper.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class KeyValueStore private constructor(
    private val keyValueDao: KeyValueDao,
    initialMap: Map<String, String>,
    private val storeScope: CoroutineScope
) {
    private val map = initialMap.toMutableMap()
    fun <T> get(key: KeyValueStoreKey<T>): T? {
        return map[key.combined]?.let(key.type::decode)
    }
    fun <T> get(key: KeyValueStoreKey<T>, default: T): T {
        return get(key) ?: default
    }

    fun <T> getAsState(key: KeyValueStoreKey<T>): KeyValueState<T> =
        KeyValueState(this, key)

    fun <T> getAsState(key: KeyValueStoreKey<T>, default: T): KeyValueStateWithDefault<T> =
        KeyValueStateWithDefault(this, key, default)

    fun <T> set(
        key: KeyValueStoreKey<T>,
        value: T
    ) {
        val encoded = key.type.encode(value)

        map[key.combined] = encoded

        storeScope.launch {
            keyValueDao.insert(
                KeyValueEntity(
                    key = key.combined,
                    value = encoded
                )
            )
        }
    }
    private fun storeMultiple(values: List<Pair<String, String>>) {
        if(values.isEmpty()) return
        map.putAll(values)

        storeScope.launch {
            keyValueDao.insertAll(
                values.map {
                    KeyValueEntity(it.first, it.second)
                }
            )
        }
    }
    inline fun <T> map(
        key: KeyValueStoreKey<T>,
        remapping: (T?) -> T
    ) {
        set(key, remapping(get(key)))
    }
    class StoreScope(private val storage: KeyValueStore) {
        private val values = mutableListOf<Pair<String, String>>()
        fun <T> get(key: KeyValueStoreKey<T>): T? = storage.get(key)
        fun <T> set(
            key: KeyValueStoreKey<T>,
            value: T
        ) {
            values.add(key.combined to key.type.encode(value))
        }
        companion object {
            fun KeyValueStore.edit(execute: StoreScope.()->Unit) {
                val scope = StoreScope(this)
                scope.execute()
                storeMultiple(scope.values)
            }
        }
    }
    internal class KeyValueState<T> internal constructor(
        private val keyValueStore: KeyValueStore,
        private val key: KeyValueStoreKey<T>,
    ) : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return keyValueStore.get(key)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            keyValueStore.set(key, value ?: return)
        }
    }
    internal class KeyValueStateWithDefault<T> internal constructor(
        private val keyValueStore: KeyValueStore,
        private val key: KeyValueStoreKey<T>,
        private val default: T
    ) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return keyValueStore.get(key) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            keyValueStore.set(key, value ?: return)
        }
    }
    companion object {
        fun createAndQueryRoomOnMain(
            keyValueDao: KeyValueDao,
            storeScope: CoroutineScope
        ): KeyValueStore {
            val initialMap = keyValueDao.getAll()
                .associate { it.key to it.value }

            return KeyValueStore(keyValueDao, initialMap, storeScope)
        }
    }
}