package com.myxoz.life.core

import java.time.LocalDate
import java.time.ZoneId

data class Rangable<T>(val from: T, val to: T, val between: Set<T>) {
    companion object {
        fun <T> ofSingle(single: T) = Rangable(single, single, setOf(single))
        fun LocalDate.rangableTo(other: LocalDate): Rangable<LocalDate> = Rangable(this, other,
            generateSequence(this) { it.plusDays(1) }
                .takeWhile { !it.isAfter(other) }
                .toSet()
        )
        fun UnixWeek.toRange() = start.rangableTo(end)
        fun Rangable<LocalDate>.startAndEndInMs(zone: ZoneId) = from.atStartAsMillis(zone) to to.atEndAsMillis(zone)
    }
}
