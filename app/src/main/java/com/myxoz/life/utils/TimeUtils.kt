package com.myxoz.life.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Long.toLocalDate(zone: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.atStartAsMillis(zone: ZoneId): Long =
    this.atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.atEndAsMillis(zone: ZoneId): Long =
    this.plusDays(1).atStartAsMillis(zone)
