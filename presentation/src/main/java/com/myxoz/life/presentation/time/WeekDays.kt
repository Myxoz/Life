package com.myxoz.life.presentation.time

object WeekDays {
    fun getWeekDayByInt(day: Int): String {
        return listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")[day]
    }
}