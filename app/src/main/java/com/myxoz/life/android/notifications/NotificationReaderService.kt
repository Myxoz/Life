package com.myxoz.life.android.notifications

import android.icu.util.Calendar
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class NotificationReaderService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // Only handle Google Wallet
        if (sbn.packageName != "de.fiduciagad.android.wlwallet") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: return

        val cents = title.substringBefore(" EUR").trim().replace(",","").toInt()

        val datePart = text.substringAfterLast(" am ").substringBefore(" Uhr").split(" um ")
        val sDate = datePart[0].split(".")
        val time = datePart[1].split(":")
        if(datePart.size != 2 || sDate.size != 3 || time.size != 2) {
            println("A payment notification couldnt be parsed, this is indication that a structural change of the notification happened")
            return
        }
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, sDate[2].toIntOrNull()?:return)
        calendar.set(Calendar.MONTH, sDate[1].toIntOrNull()?.minus(1)?:return)
        calendar.set(Calendar.DAY_OF_MONTH, sDate[0].toIntOrNull()?:return)

        calendar.set(Calendar.HOUR_OF_DAY, time[0].toIntOrNull()?:return)
        calendar.set(Calendar.MINUTE, time[1].toIntOrNull()?:return)
        calendar.set(Calendar.SECOND, 0)

        val payment = JSONObject().apply {
            put("amount", cents)
            put("timestamp", calendar.timeInMillis)
        }

        val prefs = getSharedPreferences("MainActivity", MODE_PRIVATE)
        val paymentsRaw = prefs.getString("payments", "[]")?:"[]"
        val paymentsArray = JSONArray(paymentsRaw)

        paymentsArray.put(payment)

        prefs.edit {
            putString("payments", paymentsArray.toString())
        }
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        // used for debugging reasons when its needed uncomment
//        dumpAllNotifications()
//        for(sbn in activeNotifications) {
//            this.onNotificationPosted(sbn)
//        }
    }

    private fun dumpAllNotifications() {
        val activeNotifications: Array<StatusBarNotification> = activeNotifications
        for (sbn in activeNotifications) {
            val pkg = sbn.packageName
            val id = sbn.id
            val tag = sbn.tag
            val title = sbn.notification.extras.getString("android.title")
            val text = sbn.notification.extras.getCharSequence("android.text")?.toString()
            val whenPosted = sbn.postTime

            println("Package: $pkg, ID: $id, Tag: $tag, Title: $title, Text: $text, When: $whenPosted")
        }
    }
}
