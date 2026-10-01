package com.myxoz.life.utils

import android.icu.util.Calendar
import com.myxoz.life.core.padStart


@Deprecated("Move to using java.time")
fun Long.formatMsToDuration(ignoreSeconds: Boolean=false): String {
    if(this == 0L) return if(ignoreSeconds) "0m" else "0s"
    val t = this/1000
    val h = (t/3600)
    val m = (t/60)%60
    val s = t%60
    return "" +
            (if(h!=0L) "${h}h" else "") +
            "${if(h!=0L && m!=0L) if(m<=9) " 0" else " " else ""}${if(m!=0L) "${m}m" else ""}" +
            if(!ignoreSeconds) "${if((h!=0L || m!=0L) && s<=9) " 0" else " "}${s}s" else ""
}

@Deprecated("Move to using java.time")
fun Long.formatTimeStamp(calendar: Calendar): String {
    calendar.timeInMillis = this
    return "${calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    }.${(calendar.get(Calendar.MONTH)+1).padStart(2, '0')}.${calendar.get(Calendar.YEAR)} ${
        calendar.get(Calendar.HOUR_OF_DAY).padStart(2, '0')}:${calendar.get(Calendar.MINUTE).padStart(2, '0')}:${calendar.get(Calendar.SECOND).padStart(2, '0')}"
}

@Deprecated("Move to using java.time")
fun Long.formatDayTime(calendar: Calendar): String {
    calendar.timeInMillis = this
    return "${calendar.get(Calendar.HOUR_OF_DAY).padStart(2, '0')}:${calendar.get(Calendar.MINUTE).padStart(2, '0')}"
}
@Deprecated("Move to using java.time")
fun Long.roundToNearest15Min(): Long {
    val block = 15 * 60 * 1000L // 15 minutes in ms
    return ((this + block / 2) / block) * block
}
@Deprecated("Move to using java.time")
fun Long.formatMinutesToVisual() = if(this == 0L) "" else "${if(this >= 60) this / 60 else ""}${if(this >= 60) (this%60).toInt().padStart(2,'0') else this%60}"

fun Int.formatMToDistance(): String =
    when(this) {
        in 0..1000 -> "${this}m"
        in 1000..10_000 -> "${this/1000},${(this/10)%100} km"
        else -> "${this/1000},${(this/100)%10} km"
    }