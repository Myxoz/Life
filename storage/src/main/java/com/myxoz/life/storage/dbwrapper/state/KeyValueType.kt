package com.myxoz.life.storage.dbwrapper.state

import com.myxoz.life.storage.api.LifeJSONArray
import java.time.Instant
import java.time.LocalDate

internal sealed interface KeyValueType<T> {
    fun decode(value: String): T?
    fun encode(value: T): String

    object StringType: KeyValueType<String> {
        override fun decode(value: String): String = value
        override fun encode(value: String): String = value
    }

    object IntType: KeyValueType<Int> {
        override fun decode(value: String): Int? = value.toIntOrNull()
        override fun encode(value: Int): String = value.toString()
    }

    object LongType: KeyValueType<Long> {
        override fun decode(value: String): Long? = value.toLongOrNull()
        override fun encode(value: Long): String = value.toString()
    }

    object InstantType: KeyValueType<Instant> {
        override fun decode(value: String): Instant? = value.toLongOrNull()?.let { Instant.ofEpochSecond(it) }
        override fun encode(value: Instant): String = value.epochSecond.toString()
    }

    object LocalDateType: KeyValueType<LocalDate> {
        override fun decode(value: String): LocalDate? = value.toLongOrNull()?.let { LocalDate.ofEpochDay(it) }
        override fun encode(value: LocalDate): String = value.toEpochDay().toString()
    }

    object BooleanType: KeyValueType<Boolean> {
        override fun decode(value: String): Boolean? = when (value) {
            "true" -> true
            "false" -> false
            else -> null
        }
        override fun encode(value: Boolean): String = value.toString()
    }


    object StringListType: KeyValueType<List<String>> {
        override fun decode(value: String): List<String>? =
            LifeJSONArray.safeFrom(value)?.mapIndexed { getString(it) }
        override fun encode(value: List<String>): String = LifeJSONArray.fromStrings(value).asString()
    }
}
