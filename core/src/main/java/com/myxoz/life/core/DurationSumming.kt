package com.myxoz.life.core

import kotlin.time.Duration

fun Iterable<Duration>.sum() = reduce { acc, duration -> acc + duration }