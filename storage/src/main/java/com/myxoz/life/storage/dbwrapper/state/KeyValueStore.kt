package com.myxoz.life.storage.dbwrapper.state

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class KeyValueStore private constructor(
    private val keyValuePrefs: SharedPreferences,
) {
    private val map = mutableMapOf<String, String>()
    fun <T> get(key: KeyValueStoreKey<T>): T? {
        return (map[key.combined] ?: keyValuePrefs.getString(key.combined, null))?.let(key.type::decode)
    }
    fun <T> get(key: KeyValueStoreKey<T>, default: T): T {
        return get(key) ?: default
    }

    fun <T> getAsState(key: KeyValueStoreKey<T>): KeyValueState<T> =
        KeyValueState(this, key)

    fun <T> getAsState(key: KeyValueStoreKey<T>, default: T): KeyValueStateWithDefault<T> =
        KeyValueStateWithDefault(this, key, default)

    fun <T> getAsWritableMutableState(key: KeyValueStoreKey<T>, default: T): MutableStateFlow<T> =
        PersistedMutableStateFlow(MutableStateFlow(get(key, default))) { put(key, it) }

    fun <T> getAsWritableMutableState(key: KeyValueStoreKey<T>): MutableStateFlow<T?> =
        PersistedMutableStateFlow(MutableStateFlow(get(key))) { put(key, it ?: return@PersistedMutableStateFlow) }

    fun <T> put(
        key: KeyValueStoreKey<T>,
        value: T
    ) {
        val encoded = key.type.encode(value)

        map[key.combined] = encoded

        keyValuePrefs.edit().putString(
            key.combined,
            encoded
        ).apply()
    }
    private fun storeMultiple(values: List<Pair<String, String>>) {
        if(values.isEmpty()) return
        map.putAll(values)

        keyValuePrefs.edit().apply {
            values.forEach { (key, encoded) ->
                putString(key, encoded)
            }
        }.apply()
    }
    inline fun <T> map(
        key: KeyValueStoreKey<T>,
        remapping: (T?) -> T
    ) {
        put(key, remapping(get(key)))
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
    class KeyValueState<T> internal constructor(
        private val keyValueStore: KeyValueStore,
        private val key: KeyValueStoreKey<T>,
    ) : ReadWriteProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return keyValueStore.get(key)
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            keyValueStore.put(key, value ?: return)
        }
    }
    class KeyValueStateWithDefault<T> internal constructor(
        private val keyValueStore: KeyValueStore,
        private val key: KeyValueStoreKey<T>,
        private val default: T
    ) : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return keyValueStore.get(key) ?: default
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            keyValueStore.put(key, value ?: return)
        }
    }
    private class PersistedMutableStateFlow<T>(
        private val state: MutableStateFlow<T>,
        private val persist: (T) -> Unit,
    ) : MutableStateFlow<T> by state {
        override var value: T
            get() = state.value
            set(newValue) {
                state.value = newValue
                persist(newValue)
            }

        override suspend fun emit(value: T) {
            this.value = value
        }

        override fun tryEmit(value: T): Boolean {
            this.value = value
            return true
        }

        override fun compareAndSet(expect: T, update: T): Boolean {
            val success = state.compareAndSet(expect, update)
            if (success) persist(update)
            return success
        }
    }
    companion object {
        fun createWithContext(
            context: Context,
        ): KeyValueStore {
            val prefs = context.getSharedPreferences("storage", MODE_PRIVATE)

            return KeyValueStore(prefs)
        }
    }
}