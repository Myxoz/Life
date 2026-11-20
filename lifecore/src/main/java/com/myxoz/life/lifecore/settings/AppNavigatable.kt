package com.myxoz.life.lifecore.settings

import android.app.PendingIntent

interface AppNavigatable {
    fun navigateTo(route: String): PendingIntent
}