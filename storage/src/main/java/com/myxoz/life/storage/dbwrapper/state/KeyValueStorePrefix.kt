package com.myxoz.life.storage.dbwrapper.state

class KeyValueStorePrefix internal constructor(private val prefix: String) {
    internal fun <T> keyOfType(suffix: String, type: KeyValueType<T>) = KeyValueStoreKey(prefix, suffix, type)
}
