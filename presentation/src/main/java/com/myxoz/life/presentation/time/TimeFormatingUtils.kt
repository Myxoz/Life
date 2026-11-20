package com.myxoz.life.presentation.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
fun LocalDate.formatToDate(): String = this.format(dateFormatter)

fun Instant.formatDayTime(zone: ZoneId): String = atZone(zone).format(dateFormatter)