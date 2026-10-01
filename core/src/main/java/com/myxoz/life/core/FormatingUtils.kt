package com.myxoz.life.core

inline fun String.nullIfEmpty() = if(!isEmpty()) this else null

fun Double.formatPercent(precision: Int): String = "${(this*100).toInt()}.${(this*100).toString().substringAfter(".").padStart(precision, '0').substring(0, precision)}%"

fun List<String>.joinGramatically(): String = when(size) {
    in Int.MIN_VALUE..0 -> ""
    1 -> first()
    else -> dropLast(1).joinToString(", ") + " und "+last()
}

inline fun Int?.plural(s: String) = if(this == 1) "" else s
operator fun Int.plus(other: Boolean) = this + (if(other) 1 else 0)
operator fun Boolean.plus(other: Boolean) = if(this && other) 2 else if(this || other) 1 else 0
inline fun <T> T?.def(value: T) = this ?: value
fun Int.padStart(length: Int, char: Char) = toString().padStart(length, char)
