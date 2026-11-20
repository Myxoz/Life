package com.myxoz.life.ui.options

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.icu.util.Calendar
import android.provider.CallLog
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myxoz.life.MainActivity
import com.myxoz.life.Theme
import com.myxoz.life.android.MainApplication
import com.myxoz.life.background.livenotification.LiveEventRefreshReceiver
import com.myxoz.life.state.ActivityScopedState
import com.myxoz.life.storage.api.API
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.DebugInterface
import com.myxoz.life.ui.alarm.screens.AlarmActivity
import com.myxoz.life.ui.holdToCopy
import com.myxoz.life.lifeui.theme.FontColor
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.lifeui.theme.TypoStyleOld
import com.myxoz.life.utils.formatTimeStamp
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant

@Composable
fun DebugScreen(
    db: DebugInterface,
    api: API,
    dbInterface: DatabaseInterface,
    repos: ActivityScopedState
) {
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = OldColors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var lastSavedSteps by remember { mutableLongStateOf(db.lastStepAmountSaved()) }
            var stepsAtMidnight by remember { mutableLongStateOf(db.stepsAtMidnight()) }
            var lastDateSaved by remember { mutableStateOf( db.stepsLastSaved() ) }
            var stepLog by db.stepResetLog
            //Text(
                //sprefs.all.map { "${it.key}: ${it.value}\n" }.joinToString(""),
                //    style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
            //)
            val isDebuggable = 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
            Text(
                "The current build is: ${if(isDebuggable) "Debuggable" else "Release"}",
                style = TypoStyle(Theme.primary, FontSize.MEDIUM)
            )
            Spacer(Modifier.height(5.dp))
//            Column(
//                Modifier.fillMaxWidth()
//            ) {
//                var text by remember { mutableStateOf("") }
//                var aiResp by remember { mutableStateOf("") }
//                val coroutineScope = rememberCoroutineScope()
//                InputField("", "Ask AI") {
//                    text = it
//                }
//                Text(aiResp?:"")
//                Button({
//                    coroutineScope.launch {
//                        val generativeModel = com.google.mlkit.genai.prompt.Generation.getClient()
//                        aiResp = generativeModel.generateContent(text).candidates.firstOrNull()?.text ?: ""
//                    }
//                }) {
//                    Text("Send to AI")
//                }
//            }
            Button({
                lastSavedSteps = db.lastStepAmountSaved()
                stepsAtMidnight = db.stepsAtMidnight()
                lastDateSaved = db.stepsLastSaved()
            }) {
                Text("Update steps")
            }
            val activity = LocalActivity.current
            Button({
                val repos = ((activity as? MainActivity)?.application as? MainApplication)?.dbInterface ?: return@Button
                coroutineScope.launch {
                    repos.bankingInterface.putFutureTransaction(
                        100,
                        Instant.now(),
                        false,
                        false,
                        "Debug",
                        "To test if the script is working"
                    )
                }
            }) {
                Text("Add manual transaction now")
            }
            Button({
                repos.alarmRepo.setAlarm(System.currentTimeMillis() + 10*1000L)
            }) {
                Text("Alarm in 10s")
            }
            Button({
                val intent = Intent(context, AlarmActivity::class.java)
                context.startActivity(intent)
            }) {
                Text("Alarm now")
            }
            Button({
                val intent = Intent(context, LiveEventRefreshReceiver::class.java)
                context.sendBroadcast(intent)
            }) {
                Text("Refresh live activity now")
            }
            var shouldWipeDp by remember { mutableIntStateOf(0) }
            Button({
                shouldWipeDp += 1
                if (shouldWipeDp == 10) {
                    shouldWipeDp = 0
                    coroutineScope.launch { db.resetAPI() }
                }
            }) {
                Text("Wipe whole db, click ${10 - shouldWipeDp} times")
            }
            Text(
                "Last steps saved: $lastSavedSteps",
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            Text(
                "Last steps at midnight: $stepsAtMidnight",
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            Text(
                "Last date saved: $lastDateSaved",
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            val calendar = remember { Calendar.getInstance() }
            val displayLog = stepLog?.map{ it.split(";") }?.sortedByDescending { it[2].toLong() }?.joinToString("\n") {"From ${it[0]} to ${it[1]} at ${it[2]} (${it[2].toLong().formatTimeStamp(calendar)})" }
            Text(
                displayLog ?: "none",
                Modifier
                    .holdToCopy(displayLog, coroutineScope)
                ,
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
            )
            val steps by dbInterface.stepInterface.steps.collectAsState(0L)
            Text("Steps live: $steps", style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM))
            var renderCallLogs by remember { mutableStateOf(false) }
            Button({renderCallLogs = true}) {
                Text("Render call logs")
            }
            val allSharedPreferences = remember { File(context.dataDir, "shared_prefs").list() }
            allSharedPreferences?.forEach {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Theme.primary)
                Column (
                    Modifier.background(Theme.surface)
                ) {
                    /*
                    pref.all.forEach { (key, any) ->
                        Row {
                            Text(key, color = Theme.primary)
                            Text(any.toString(), Modifier.weight(1f), color = Theme.primary)
                            Icon(
                                painterResource(R.drawable.delete),
                                "delete",
                                Modifier
                                    .clickable{
                                        pref.edit {
                                            remove(key)
                                        }
                                    }
                                ,
                                Theme.primary)
                        }
                    }
                     */
                }
            }
        }
    }
}

@Composable
fun CallLogDumpDebug() {
    val context = LocalContext.current
    val callLogs = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val calendar = Calendar.getInstance()

            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val name = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME))
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val countryIso = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.COUNTRY_ISO))
                val isRead = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.IS_READ))
                val features = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.FEATURES))
                val accountId = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_ID))
                val accountComponent = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME))
                val postDial = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.POST_DIAL_DIGITS))
                val viaNumber = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.VIA_NUMBER))
                val presentation = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER_PRESENTATION))
                val geocodedLocation = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION))
                val dataUsage = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATA_USAGE))
                val transcription = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.TRANSCRIPTION))
                if (duration == 0L) continue
                if (number.isNullOrBlank()) continue
                calendar.timeInMillis = date
                val formattedTime = date.formatTimeStamp(calendar)

                callLogs.add(
                    """
    Number: $number
    Name: $name
    Type: $type
    Duration: ${duration / 60}m ${duration % 60}s
    Date: $formattedTime
    Country ISO: $countryIso
    Geo: $geocodedLocation
    Read: $isRead
    Features: $features
    Presentation: $presentation
    Account ID: $accountId
    Account Component: $accountComponent
    Via: $viaNumber
    Post Dial: $postDial
    Data Usage: $dataUsage
    Transcription: $transcription
    --------------------------------
    """.trimIndent()
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        callLogs.forEach {
            Text(
                text = it,
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
            )
        }
    }
}
