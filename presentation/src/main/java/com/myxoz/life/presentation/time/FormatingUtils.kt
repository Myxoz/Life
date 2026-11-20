package com.myxoz.life.presentation.time

fun List<String>.joinGramatically(): String = when(size) {
    in Int.MIN_VALUE..0 -> ""
    1 -> first()
    else -> dropLast(1).joinToString(", ") + " und "+last()
}

inline fun Int?.plural(s: String) = if(this == 1) "" else s
