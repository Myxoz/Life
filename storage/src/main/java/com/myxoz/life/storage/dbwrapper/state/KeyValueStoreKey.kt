package com.myxoz.life.storage.dbwrapper.state

class KeyValueStoreKey<T> internal constructor(
    prefix: String,
    suffix: String,
    internal val type: KeyValueType<T>
) {
    internal val combined = "$prefix.$suffix"
}
