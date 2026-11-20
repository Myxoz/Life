package com.myxoz.life.calendar

import com.myxoz.life.storage.interfaces.DatabaseInterface

class CalendarRepo(
    private val repos: DatabaseInterface,
    private val calendarSuper: CalendarSuper
)