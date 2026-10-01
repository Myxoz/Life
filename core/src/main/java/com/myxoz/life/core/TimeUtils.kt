package com.myxoz.life.core

import java.time.Instant
import java.time.LocalDate
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

operator fun Instant.plus(other: Duration): Instant =
    this.plus(other.inWholeMilliseconds, ChronoUnit.MILLIS)

operator fun Instant.minus(other: Duration): Instant =
    this.minus(other.inWholeMilliseconds, ChronoUnit.MILLIS)

operator fun Instant.minus(other: Instant): Duration =
    (this.toEpochMilli() - other.toEpochMilli()).milliseconds

fun Instant.toLocalDate(zone: ZoneId): LocalDate = atZone(zone).toLocalDate()

fun Long?.toInstant(): Instant? = Instant.ofEpochMilli(this ?: return null)
fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)
