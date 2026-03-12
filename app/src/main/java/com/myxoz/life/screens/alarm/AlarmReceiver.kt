package com.myxoz.life.screens.alarm

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.myxoz.life.R

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("FullScreenIntentPolicy")
    override fun onReceive(context: Context, intent: Intent) {

        val intent = Intent(context, AlarmActivity::class.java)

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(R.drawable.alarm_clock)
            .setContentTitle("Alarm")
            .setContentText("Wake up")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(161, notification)
    }
}
