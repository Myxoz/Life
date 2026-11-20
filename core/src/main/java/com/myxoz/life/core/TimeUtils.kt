package com.myxoz.life.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Deprecated("Migrate to using Instants")
fun LocalDate.atStartAsMillis(zone: ZoneId): Long =
    this.atStartOfDay(zone).toInstant().toEpochMilli()

@Deprecated("Migrate to using Instants")
fun LocalDate.atEndAsMillis(zone: ZoneId): Long =
    this.plusDays(1).atStartAsMillis(zone)

fun LocalDate.atStartAsInstant(zone: ZoneId): Instant =
    this.atStartOfDay(zone).toInstant()

fun LocalDate.atEndAsInstant(zone: ZoneId): Instant =
    this.plusDays(1).atStartAsInstant(zone)

operator fun Instant.plus(other: Duration): Instant =
    plus(other.inWholeMilliseconds, ChronoUnit.MILLIS)

operator fun Instant.minus(other: Duration): Instant =
    minus(other.inWholeMilliseconds, ChronoUnit.MILLIS)

operator fun Instant.minus(other: Instant): Duration =
    (toEpochMilli() - other.toEpochMilli()).milliseconds

fun Instant.toLocalDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

fun <T: Comparable<T>> min(a: T, b: T): T = if(a > b) a else b

fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
private const val block = 15 * 60 * 1000L // 15 mintes in ms
fun Instant.roundToNearest15Min(): Instant {
    return ((toEpochMilli() + block / 2) / block * block).toInstant()
}
