package com.myxoz.life.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
fun LocalDate.formatToDate() = this.format(dateFormatter)
