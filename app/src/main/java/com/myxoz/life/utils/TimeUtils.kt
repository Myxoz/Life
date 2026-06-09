package com.myxoz.life.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun Long.toLocalDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.atStartAsMillis(zone: ZoneId): Long =
    this.atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.atEndAsMillis(zone: ZoneId): Long =
    this.plusDays(1).atStartAsMillis(zone)

fun LocalDate.datesThrough(other: LocalDate): List<LocalDate> =
    generateSequence(this) { it.plusDays(1) }
        .takeWhile { !it.isAfter(other) }
        .toList()

fun LocalDate.getAge(birth: LocalDate) = year - birth.year + if((monthValue > birth.monthValue) || (monthValue == birth.monthValue && dayOfMonth >= birth.dayOfMonth /* If equals, birthday is today, Happy Birthday */)) 0 else -1
class UnixWeek(val week: Long) {
    val start: LocalDate = epochMonday.plusWeeks(week)
    val end: LocalDate = start.plusDays(6)
    fun containedDays() = start.datesThrough(end)
    override fun equals(other: Any?) = other is UnixWeek && other.week == week
    override fun hashCode() = week.hashCode()
    companion object {
        inline fun of(epochDay: LocalDate) = epochDay.asUnixWeek()
        val epochMonday: LocalDate = LocalDate.of(1970, 1, 5)
    }
}
fun LocalDate.asUnixWeek(): UnixWeek = UnixWeek(
    ChronoUnit.DAYS.between(
        UnixWeek.epochMonday,
        this
    ) / 7
)