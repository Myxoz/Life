package com.myxoz.life.utils

import android.icu.util.Calendar

fun Long.formatTimeStamp(calendar: Calendar): String {
    calendar.timeInMillis = this
    return "${calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    }.${(calendar.get(Calendar.MONTH)+1).padStart(2, '0')}.${calendar.get(Calendar.YEAR)} ${
        calendar.get(Calendar.HOUR_OF_DAY).padStart(2, '0')}:${calendar.get(Calendar.MINUTE).padStart(2, '0')}:${calendar.get(Calendar.SECOND).padStart(2, '0')}"
}
fun Long.formatMinutes(calendar: Calendar): String {
    calendar.timeInMillis = this
    return "${calendar.get(Calendar.HOUR_OF_DAY).padStart(2, '0')}:${calendar.get(Calendar.MINUTE).padStart(2, '0')}"
}
fun Int.padStart(length: Int, char: Char) = toString().padStart(length, char)
fun Long.roundToNearest15Min(): Long {
    val block = 15 * 60 * 1000L // 15 minutes in ms
    return ((this + block / 2) / block) * block
}
fun Long.formatMinutesToVisual() = "${this / 60}${(this%60).toInt().padStart(2,'0')}"