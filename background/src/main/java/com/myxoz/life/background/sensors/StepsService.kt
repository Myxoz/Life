package com.myxoz.life.background.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifecore.settings.AppNavigatable
import com.myxoz.life.lifecore.settings.AppPermissionable
import com.myxoz.life.lifecore.settings.Feature
import com.myxoz.life.lifecore.settings.Permission
import com.myxoz.life.presentation.notifications.StepsServiceRepresentation
import com.myxoz.life.storage.DatabaseInterfacable
import com.myxoz.life.storage.interfaces.StepInterface

class StepsService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var stepSensor: Sensor
    private lateinit var stepInterface: StepInterface
    private lateinit var settingsChecker: Permission.SettingsChecker
    private lateinit var navigatable: AppNavigatable

    override fun onCreate() {
        super.onCreate()
        stepInterface = (applicationContext as DatabaseInterfacable).dbInterface.stepInterface
        settingsChecker = (applicationContext as AppPermissionable).settingsChecker
        navigatable = (applicationContext as AppNavigatable)
        if(!Feature.StepCounting.enabledAssured(settingsChecker)) return stopSelf()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!!
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        try {
            startForeground(1, createNotification())
        } catch (_: SecurityException) {
            stopSelf()
            return
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.getBooleanExtra("stop", false) == true) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceReboot = event.values[0].toLong()
            stepInterface.updateSteps(totalStepsSinceReboot)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun createNotification(): Notification {
        val channelId = "steps_channel"
        val channel = NotificationChannel(
            channelId, StepsServiceRepresentation.CHANNELNAME,
            NotificationManager.IMPORTANCE_MIN
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
        val pendingIntent = navigatable.navigateTo(NavPath.DAY_OVERVIEW.with(0))

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(StepsServiceRepresentation.NOTIFICATIONTITLE)
            .setContentText(StepsServiceRepresentation.NOTIFICATIONCONTENT)
            .setSmallIcon(StepsServiceRepresentation.NOTIFICATIONICON)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object {
        fun createNotificationChannel(nm: NotificationManager) {
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
    }
}