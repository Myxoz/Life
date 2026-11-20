package com.myxoz.life.core

import java.time.Instant

fun dif(one: Instant, other: Instant) = if(one > other) one - other else other - one