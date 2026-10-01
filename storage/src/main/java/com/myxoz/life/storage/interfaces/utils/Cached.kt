package com.myxoz.life.storage.interfaces.utils

sealed class Cached<out T: Any>(val value: T?) {
    data class Value<T: Any>(val content: T) : Cached<T>(content)
    object Null: Cached<Nothing>(null)
    companion object {
        fun <T> by(it: T?) = if(it != null) Value(it) else Null
        val <T: Any> T?.cached: Cached<T>
            get() = if(this != null) Value(this) else Null
    }
}
