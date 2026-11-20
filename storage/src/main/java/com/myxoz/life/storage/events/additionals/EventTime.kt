package com.myxoz.life.storage.events.additionals

import com.myxoz.life.core.minus
import com.myxoz.life.core.toLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class EventTime(
    val start: Instant,
    val end: Instant,
    val unspecifiedStart: Boolean,
    val unspecifiedLength: Boolean
) {
    fun stetchedDays(zone: ZoneId): List<LocalDate>  {
        val other = end.toLocalDate(zone)
        return generateSequence(start.toLocalDate(zone)) { it.plusDays(1) }
            .takeWhile { !it.isAfter(other) }
            .toList()
    }
    fun length() = end - start
    fun overlaps(other: EventTime) = other.start < end && other.end > start
    fun overlaps(otherStart: Instant, otherEnd: Instant) = otherStart < end && otherEnd > start
}