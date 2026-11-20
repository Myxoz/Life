package com.myxoz.life.presentation.time

import java.time.Month

fun getShortMonthByCalendarMonth(month: Month): String{
    return listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Juni", "Juli", "Aug", "Sep", "Okt", "Nov", "Dez")[month.value - 1]
}


