package com.myxoz.life.lifecore.settings

import android.content.Context

interface SpecificPermissionChecker {
    fun isNotificationListenerEnabled(context: Context): Boolean
}