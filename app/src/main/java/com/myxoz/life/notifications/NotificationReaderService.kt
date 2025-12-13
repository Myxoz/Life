package com.myxoz.life.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class NotificationReaderService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Only handle Google Wallet
        if (sbn.packageName != "com.google.android.apps.walletnfcrel") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return
        val timestamp = sbn.postTime // Unix timestamp in ms

        // Parse amount (before first space, remove currency and dot)
        val amountRaw = text.split(" ").firstOrNull()?.replace("[^0-9]".toRegex(), "") ?: "0"
        val amountCents = amountRaw.toIntOrNull() ?: 0

        // Create new payment JSON object
        val payment = JSONObject().apply {
            put("to", title)
            put("amount", amountCents)
            put("timestamp", timestamp)
        }

        // Get prefs named after local class name (your style)
        val prefs = getSharedPreferences("MainActivity", MODE_PRIVATE)
        val paymentsRaw = prefs.getString("payments", "[]")?:"[]"
        val paymentsArray = JSONArray(paymentsRaw)

        // Append new entry
        paymentsArray.put(payment)

        // Save back
        prefs.edit {
            putString("payments", paymentsArray.toString())
        }

        Log.d("NotifReader", "Saved payment: $payment")
    }

}
