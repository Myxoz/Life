package com.myxoz.life.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.SavedStateHandle
import com.myxoz.life.ui.NavPath
import com.myxoz.life.waitUntilNextMidnight
import kotlinx.coroutines.isActive
import java.time.LocalDate

fun LocalDate.getAge(birth: LocalDate) = year - birth.year + if((monthValue > birth.monthValue) || (monthValue == birth.monthValue && dayOfMonth >= birth.dayOfMonth /* If equals, birthday is today, Happy Birthday */)) 0 else -1
fun SavedStateHandle.getDate(path: NavPath.SingleParamaterizedPath): LocalDate {
    val epochDay = this.get<Long>(path.parameterName)
    return if(epochDay == null) LocalDate.now() else LocalDate.ofEpochDay(epochDay)
}

@Composable
fun updatingToday(): State<LocalDate> {
    return produceState(LocalDate.now()) {
        while(isActive) {
            waitUntilNextMidnight()
            value = LocalDate.now()
        }
    }
}