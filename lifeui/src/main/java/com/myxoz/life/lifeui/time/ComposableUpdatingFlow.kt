package com.myxoz.life.lifeui.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import java.time.LocalDate

@Composable
fun updatingToday(): State<LocalDate> {
    return produceState(LocalDate.now()) {
        while(true) {
            waitUntilNextMidnight()
            value = LocalDate.now()
        }
    }
}

