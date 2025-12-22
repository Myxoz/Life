package com.myxoz.life.options

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.myxoz.life.LocalAPI
import com.myxoz.life.LocalStorage
import com.myxoz.life.MainActivity
import com.myxoz.life.R
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.api.Syncable
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.viewmodels.ContactsViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DebugScreen(){
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            val context = LocalContext.current
            val db = LocalStorage.current
            val api = LocalAPI.current
            val coroutineScope = rememberCoroutineScope()
            val prefs = remember { context.getSharedPreferences("steps", MODE_PRIVATE) }
            val sprefs = remember { context.getSharedPreferences("MainActivity", MODE_PRIVATE) }
            var lastSavedSteps by remember { mutableLongStateOf(prefs.getLong("saved_steps", 0L)) }
            var stepsAtMidnight by remember {
                mutableLongStateOf(
                    prefs.getLong(
                        "steps_at_midnight",
                        0L
                    )
                )
            }
            var lastDateSaved by remember {
                mutableLongStateOf(
                    prefs.getLong(
                        "last_steps_date",
                        0L
                    )
                )
            }
            Text(
                sprefs.all.map { "${it.key}: ${it.value}\n" }.joinToString(""),
                style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            Spacer(Modifier.height(5.dp))
            Button({
                lastSavedSteps = prefs.getLong("saved_steps", 0L)
                stepsAtMidnight = prefs.getLong("steps_at_midnight", 0L)
                lastDateSaved = prefs.getLong("last_steps_date", 0L)
            }) {
                Text("Update steps")
            }
            Button({
                coroutineScope.launch {
                    val allPeople = db.people.getAllPeople()
                    allPeople.forEach {
                        PersonSyncable.from(db, it.copy(phoneNumber = it.phoneNumber?.replace(" ", "")))
                            .saveAndSync(db)
                    }
                }
            }) {
                Text("Fix Phone numbers to remove spaces")
            }
            Button({
                coroutineScope.launch {
                    val newConactsViewModel = ContactsViewModel()
                    newConactsViewModel.refetchLifeContacts(db)
                    newConactsViewModel.fetchDeviceContacts(db, context)
                    newConactsViewModel.lifeContacts.value.forEach {
                        val socials = it.socials.toMutableList()
                        if(socials.none { p -> p.platform == PersonSyncable.Companion.Platform.WhatsApp }) {
                            if(newConactsViewModel.deviceContacts.value.find { lc ->
                                    it.phoneNumber?.trim()?.replace("\\D".toRegex(), "") == lc.phoneNumber?.trim()?.replace("\\D".toRegex(), "")
                                }?.socials?.any { it.platform == PersonSyncable.Companion.Platform.WhatsApp }==true)  {
                                socials.add(PersonSyncable.Companion.Socials(PersonSyncable.Companion.Platform.WhatsApp, "WhatsApp"))
                                PersonSyncable(
                                    it.id,
                                    it.name,
                                    it.fullName,
                                    it.phoneNumber,
                                    it.iban,
                                    it.home,
                                    it.birthday,
                                    socials,
                                ).saveAndSync(db)
                            }
                        }
                    }
                }
            }) {
                Text("Fix whatsapp contacts (outdated)")
            }
            var shouldWipeDp by remember { mutableIntStateOf(0) }
            Button({
                shouldWipeDp += 1
                if (shouldWipeDp == 10) {
                    shouldWipeDp = 0
                    coroutineScope.launch {
                        sprefs.edit {
                            putLong("last_update", 0)
                        }
                        db.cleanupDb.clearAllExceptPersistent()
                        File(context.filesDir, Syncable.SpecialSyncablesIds.PROFILEPICTURE.toString()).deleteRecursively() // Delete Profile Pics
                        api.lastUpdate = 0
                    }
                }
            }) {
                Text("Wipe whole db, click ${10 - shouldWipeDp} times")
            }
            Text(
                "Last steps saved: $lastSavedSteps",
                style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            Text(
                "Last steps at midnight: $stepsAtMidnight",
                style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            Text(
                "Last date saved: $lastDateSaved",
                style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            StepCounterText {
                Text("Steps live: $it", style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM))
            }
        }
    }
}
@Composable
fun StepCounterText(content: @Composable (Long)->Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    val midnight = remember { context.getSharedPreferences("steps", MODE_PRIVATE).getLong("steps_at_midnight", 0L) }

    var steps: Long by rememberSaveable { mutableLongStateOf(0L) }

    DisposableEffect(stepSensor) {
        if (stepSensor == null) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    steps = event.values[0].toLong()-midnight
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }

    content(steps)
}