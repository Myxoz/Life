package com.myxoz.life.background

import com.myxoz.life.background.alarm.AlarmScheduler

interface ContainsAlarmScheduler {
    val alarmScheduler: AlarmScheduler
}