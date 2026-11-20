package com.myxoz.life.core

inline fun String.nullIfEmpty() = if(!isEmpty()) this else null