package com.myxoz.life.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class UnixWeek(val week: Long) {
    val start: LocalDate = epochMonday.plusWeeks(week)
    val end: LocalDate = start.plusDays(6)
    override fun equals(other: Any?) = other is UnixWeek && other.week == week
    override fun hashCode() = week.hashCode()
    companion object {
        inline fun of(epochDay: LocalDate) = epochDay.asUnixWeek()
        private val epochMonday: LocalDate = LocalDate.of(1970, 1, 5)
        fun LocalDate.asUnixWeek(): UnixWeek = UnixWeek(
            ChronoUnit.DAYS.between(
                UnixWeek.epochMonday,
                this
            ) / 7
        )
    }
}

