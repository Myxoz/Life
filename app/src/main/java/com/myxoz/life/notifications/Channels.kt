package com.myxoz.life.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

fun createNotificationChannels(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java)

    val stepsChannel = NotificationChannel(
        "steps_channel",
        "Step Tracking",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Notifications for step counting service"
        enableVibration(false)
        setShowBadge(false)
    }

    nm.createNotificationChannel(stepsChannel)
    nm.deleteNotificationChannel("step_tracking")
}