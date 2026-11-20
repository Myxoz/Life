package com.myxoz.life.utils

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate

fun LocalDate.getAge(birth: LocalDate) = year - birth.year + if((monthValue > birth.monthValue) || (monthValue == birth.monthValue && dayOfMonth >= birth.dayOfMonth /* If equals, birthday is today, Happy Birthday */)) 0 else -1
fun SavedStateHandle.getDate(path: SingleParamaterizedPath): LocalDate {
    val epochDay = this.get<Long>(path.parameterName)
    return if(epochDay == null) LocalDate.now() else LocalDate.ofEpochDay(epochDay)
}