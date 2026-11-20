package com.myxoz.life.presentation.time

import kotlin.time.Duration

@Deprecated("Move to using kotlin.duration")
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

fun Duration.formatMsToDuration(ignoreSeconds: Boolean=false): String {
    if(inWholeMilliseconds == 0L) return if(ignoreSeconds) "0m" else "0s"
    val hours = inWholeHours % 24
    val hasHours = hours != 0L
    val minute = inWholeMinutes % 60
    val hasMinutes = minute != 0L
    val seconds = inWholeSeconds % 60
    val hasSeconds = seconds != 0L
    return buildString {
        if(hasHours) append("${hours}h")
        if(hasHours && hasMinutes) append(" ")
        if(hasMinutes) {
            if(minute<=9) append(" 0") else append(" ")
            append("${minute}m")
        }
        if(ignoreSeconds) return@buildString
        if((hasHours || hasMinutes) && hasSeconds) append(" ")
        if(hasSeconds) {
            if(seconds<=9) append(" 0") else append(" ")
            append("${seconds}m")
        }
    }
}