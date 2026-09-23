package com.myxoz.life.utils

inline fun <reified T: Enum<T>> T.nextOrFirst(): T {
    val values = enumValues<T>()
    val nextOrdinal = (ordinal + 1) % values.size
    return values[nextOrdinal]
}
inline fun <reified T: Enum<T>> T.previousOrLast(): T {
    val values = enumValues<T>()
    val nextOrdinal =  if ((ordinal - 1) < 0) values.size -1 else (ordinal-1)
    return values[nextOrdinal]
}
