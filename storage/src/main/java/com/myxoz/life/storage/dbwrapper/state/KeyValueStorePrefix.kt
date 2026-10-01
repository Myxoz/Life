package com.myxoz.life.storage.dbwrapper.state

internal class KeyValueStorePrefix(private val prefix: String) {
    fun <T> keyOfType(suffix: String, type: KeyValueType<T>) = KeyValueStoreKey(prefix, suffix, type)
}
