package com.myxoz.life.background.notifications

import android.app.NotificationManager
import android.content.Context
import com.myxoz.life.background.livenotification.LiveRescheduling
import com.myxoz.life.background.sensors.StepsService

fun createNotificationChannels(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)
    LiveRescheduling.createNotificationChannel(nm)
    StepsService.createNotificationChannel(nm)
}