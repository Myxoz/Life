package com.myxoz.life.core

inline fun <T> T?.def(value: T) = this ?: value
