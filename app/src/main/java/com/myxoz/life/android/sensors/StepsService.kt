package com.myxoz.life.android.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myxoz.life.MainActivity
import com.myxoz.life.R
import com.myxoz.life.repositories.MainApplication
import com.myxoz.life.repositories.StepRepo
import com.myxoz.life.screens.NavPath
import com.myxoz.life.viewmodels.Settings

class StepsService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var stepSensor: Sensor
    private lateinit var repository: StepRepo
    private lateinit var permissionChecker: Settings.Permission.PermissionChecker

    override fun onCreate() {
        super.onCreate()
        val appRepo = (applicationContext as MainApplication).repositories
        repository = appRepo.stepRepo
        permissionChecker = appRepo.permissionChecker
        if(!Settings.Feature.StepCounting.hasAssured(permissionChecker)) return stopSelf()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceReboot = event.values[0].toLong()
            repository.updateSteps(totalStepsSinceReboot)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun createNotification(): Notification {
        val channelId = "steps_channel"
        val channel = NotificationChannel(
            channelId, "Step tracking",
            NotificationManager.IMPORTANCE_MIN
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            navigateTo(NavPath.DAY_OVERVIEW.with(0))
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Step tracking active")
            .setContentText("Monitoring your steps in background")
            .setSmallIcon(R.drawable.walk)
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
}
fun Intent.navigateTo(route: String){
    putExtra("targetRoute", route) // Today
}