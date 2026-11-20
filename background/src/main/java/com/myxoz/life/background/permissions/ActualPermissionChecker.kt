package com.myxoz.life.background.permissions

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import com.myxoz.life.background.notifications.NotificationReaderService
import com.myxoz.life.lifecore.settings.SpecificPermissionChecker

class ActualPermissionChecker: SpecificPermissionChecker {
    override fun isNotificationListenerEnabled(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val component = ComponentName(context, NotificationReaderService::class.java)
        return nm.isNotificationListenerAccessGranted(component)
    }
}