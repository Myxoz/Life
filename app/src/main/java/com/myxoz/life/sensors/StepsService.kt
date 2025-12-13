package com.myxoz.life.sensors

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.myxoz.life.MainActivity
import com.myxoz.life.R
import com.myxoz.life.dbwrapper.DatabaseProvider
import com.myxoz.life.dbwrapper.ProposedStepsDao
import com.myxoz.life.dbwrapper.ProposedStepsEntity
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class StepsService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var prefs: SharedPreferences
    private var dao: ProposedStepsDao? = null

    private var lastSavedSteps = 0L
    private var stepsAtMidnight = 0L
    private var lastDateSaved = 0L
    private var totalStepsSinceReboot = 0L
    private var lastRebootTs = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("steps", MODE_PRIVATE)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lastSavedSteps = prefs.getLong("saved_steps", 0L)
        stepsAtMidnight = prefs.getLong("steps_at_midnight", 0L)
        lastDateSaved = prefs.getLong("last_steps_date", 0L)
        val currentRebootTs = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        lastRebootTs = prefs.getLong("last_reboot_ts", 0L)
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if(currentRebootTs - lastRebootTs > 30000) { // Has rebooted
            if(lastDateSaved < LocalDate.now().toEpochDay()){ // We bridged midnight and this is now a new day
                enterCurrentDay()
            } else {
                setAndSaveFields(
                    0L,
                    -lastSavedSteps+stepsAtMidnight,
                    System.currentTimeMillis() - SystemClock.elapsedRealtime()
                )
            }
        }
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            totalStepsSinceReboot = event.values[0].toLong()
            if(LocalDate.now().toEpochDay() > lastDateSaved) {
                enterCurrentDay()
            }
            if (totalStepsSinceReboot - lastSavedSteps > 10) {
                setAndSaveFields(totalStepsSinceReboot, null)
            }
        }
    }

    fun setAndSaveFields(totalStepsSinceReboot: Long?, stepsAtMidnight: Long?, lastRebootTs: Long?=null){
        totalStepsSinceReboot?.let {
            this.totalStepsSinceReboot = it
            lastSavedSteps = it
            lastDateSaved = LocalDate.now().toEpochDay()
            prefs.edit{
                putLong("last_steps_date", LocalDate.now().toEpochDay())
                putLong("saved_steps", it)
            }
        }
        stepsAtMidnight?.let {
            this.stepsAtMidnight = it
            prefs.edit{
                putLong("steps_at_midnight", it)
            }
        }
        lastRebootTs?.let {
            this.lastRebootTs = it
            prefs.edit{
                putLong("last_reboot_ts", it)
            }
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
            navigateTo("day/0/overview")
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
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun enterCurrentDay(){
        val db = DatabaseProvider.getDatabase(applicationContext)
        dao = db.proposedStepsDao()
        runBlocking {
            (dao ?: db.proposedStepsDao()).insertSteps(
                ProposedStepsEntity(
                    lastDateSaved,
                    (lastSavedSteps-stepsAtMidnight).toInt()
                )
            )
            setAndSaveFields(totalStepsSinceReboot, totalStepsSinceReboot, System.currentTimeMillis() - SystemClock.elapsedRealtime())
        }
    }
}
fun Intent.navigateTo(route: String){
    putExtra("targetRoute", route) // Today
}