package com.myxoz.life.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.SavedStateHandle
import com.myxoz.life.ui.NavPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

fun SavedStateHandle.getDate(path: NavPath.SingleParamaterizedPath): LocalDate {
    val epochDay = this.get<Long>(path.parameterName)
    return if(epochDay == null) LocalDate.now() else LocalDate.ofEpochDay(epochDay)
}

fun timeUntilNextMidnight(): Duration {
    val now = LocalDateTime.now()
    val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
    // Very generous 10ms for LocalDate.now() to update. Very likely never ever necessary,
    // but the user doesn't notice anything so we do not care
    return java.time.Duration.between(now, tomorrow).toMillis().milliseconds + 10.milliseconds
}
suspend fun waitUntilNextMidnight(){
    delay(timeUntilNextMidnight())
}

@Composable
fun updatingToday(): State<LocalDate> {
    return produceState(LocalDate.now()) {
        while(isActive) {
            waitUntilNextMidnight()
            value = LocalDate.now()
        }
    }
}