package com.myxoz.life.events.additionals

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.min

open class DefinedDurationEvent(val start: Long, val end: Long) {
    fun length() = (end - start).coerceAtLeast(15*1000L*60)
    fun getDurationInRange(startOfDay: Long, endOfDay: Long): Long = min(length(), min(endOfDay - start, end - startOfDay))
    fun getBlockHeight(startOfDay: Long, endOfDay: Long): Int = (getDurationInRange(startOfDay, endOfDay)/(900*1000)).toInt()
    fun getBlockLength() = (length() / (900*1000)).toInt()
    fun getTopPadding(oneHour: Dp, startOfDay: Long) = ((this.start - startOfDay).coerceAtLeast(0L) / (3600 * 1000L).toFloat()) * oneHour + 1.dp
    fun getHeightDp(oneHour: Dp, startOfDay: Long, endOfDay: Long) = oneHour*(getBlockHeight(startOfDay, endOfDay)/4f) - 1.dp
    fun overlaps(other: DefinedDurationEvent): Boolean = start < other.end && end > other.start
    fun getAllStrechedDays(zone: ZoneId): List<LocalDate> {
        val startDate = Instant.ofEpochSecond(start/1000).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochSecond(end/1000-1).atZone(zone).toLocalDate()

        val dates = mutableListOf<LocalDate>()
        var current = startDate

        while (!current.isAfter(endDate)) {
            dates += current
            current = current.plusDays(1)
        }

        return dates
    }
}