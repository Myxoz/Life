package com.myxoz.life.lifecore.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend fun waitUntilNextMidnight(){
    delay(timeUntilNextMidnight())
}
private fun timeUntilNextMidnight(): Duration {
    val now = LocalDateTime.now()
    val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
    // Very generous 10ms for LocalDate.now() to update. Very likely never ever necessary,
    // but the user doesn't notice anything so we do not care
    return java.time.Duration.between(now, tomorrow).toMillis().milliseconds + 10.milliseconds
}

fun updatingTodayFlow(): Flow<LocalDate> = flow {
    while(true) {
        emit(LocalDate.now())
        waitUntilNextMidnight()
    }
}
fun updatingMinuteFlow(): Flow<Instant> = flow {
    emit(Instant.now())
    while(true) {
        delay((60 * 1000L - (System.currentTimeMillis() % (60 * 1000L))).milliseconds)
        emit(Instant.now())
    }
}
