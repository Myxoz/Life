package com.myxoz.life.storage.stored

import com.myxoz.life.storage.dbwrapper.state.KeyValueStoreKey
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType
import java.time.LocalDate

sealed class CalendarStored<T>(
    val key: KeyValueStoreKey<T>
) {
    companion object {
        private val keyPrefix = KeyValueStorePrefix("calendar")
    }
    object DisplayedDays: CalendarStored<Int>(keyPrefix.keyOfType("displayed_days", KeyValueType.IntType))
    object VisibleDate: CalendarStored<LocalDate>(keyPrefix.keyOfType("visible_date", KeyValueType.LocalDateType))

}