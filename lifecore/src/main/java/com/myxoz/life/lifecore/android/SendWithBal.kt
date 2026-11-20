package com.myxoz.life.lifecore.android

import android.app.ActivityOptions
import android.app.PendingIntent
import android.os.Build

fun PendingIntent.sendWithBal() {
    val options = ActivityOptions.makeBasic().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
    }.toBundle()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        this.send(options)
    } else {
        this.send()
    }
}
