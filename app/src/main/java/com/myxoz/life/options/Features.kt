package com.myxoz.life.options

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalAPI
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.subscreens.edgeToEdgeGradient
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun SettingsPermissionComposable() {
    Scaffold(
        Modifier.fillMaxSize(),
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val settings = LocalSettings.current
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            Text("Berechtigungen", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth(.95f))
            Column(
                Modifier
                    .fillMaxWidth(.95f)
                    .background(Colors.SECONDARY, RoundedCornerShape(30.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PermissionComposable(
                    settings.permissions.usageStats
                ) {
                    settings.permissions.usageStats.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Colors.DIVIDERS)
                PermissionComposable(
                    settings.permissions.physicalActivity
                ) {
                    settings.permissions.physicalActivity.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Colors.DIVIDERS)
                PermissionComposable(
                    settings.permissions.contacts
                ) {
                    settings.permissions.contacts.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Colors.DIVIDERS)
                PermissionComposable(
                    settings.permissions.internet
                ) {
                    settings.permissions.internet.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Colors.DIVIDERS)
                PermissionComposable(
                    settings.permissions.phone
                ) {
                    settings.permissions.phone.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Colors.DIVIDERS)
                PermissionComposable(
                    settings.permissions.readNotifications
                ) {
                    settings.permissions.readNotifications.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Colors.DIVIDERS)
                PermissionComposable(
                    settings.permissions.postNotifications
                ) {
                    settings.permissions.postNotifications.set(it)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Features", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth(.95f))
            Column(
                Modifier
                    .fillMaxWidth(.95f)
                    .background(Colors.SECONDARY, RoundedCornerShape(30.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FeatureItem(settings.features.callFromLife) {
                    settings.features.callFromLife.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Colors.DIVIDERS)
                FeatureItem(settings.features.addNewPerson) {
                    settings.features.addNewPerson.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Colors.DIVIDERS)
                FeatureItem(settings.features.stepCounting) {
                    settings.features.stepCounting.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Colors.DIVIDERS)
                FeatureItem(settings.features.screentime) {
                    settings.features.screentime.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Colors.DIVIDERS)
                FeatureItem(settings.features.autoDetectSleep) {
                    settings.features.autoDetectSleep.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Colors.DIVIDERS)
                FeatureItem(settings.features.readPaymentNotifications) {
                    settings.features.readPaymentNotifications.set(it)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Colors.DIVIDERS)
                val api = LocalAPI.current
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val clipboard = LocalClipboardManager.current
                FeatureItem(settings.features.syncWithServer) {
                    coroutineScope.launch {
                        if(it){
                            if(api.testSign()) {
                                Toast.makeText(context, "Verifiziert", Toast.LENGTH_SHORT).show()
                                settings.features.syncWithServer.set(true)
                            } else {
                                Toast.makeText(context, "Publickey kopiert; Keine Verifizierung möglich", Toast.LENGTH_LONG).show()
                                clipboard.setText(AnnotatedString(api.security.getBase64Public()))
                            }
                        } else {
                            settings.features.syncWithServer.set(false)
                        }
                    }
                }
            }
            Spacer(Modifier.height(innerPadding.calculateTopPadding()))
        }
//            PermissionComposable(
//                "Read Notifications",
//                "Used for catching Google Wallet Notifications and then displaying them in the calendar as well as merging these with actual the bank transactions"
//            ) {
//                context.startActivity(
//                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                )
//            }
//            HorizontalDivider()
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                HorizontalDivider()
//                PermissionComposable(
//                    "Notifications",
//                    "Used for displaying persistant notification for Steps Counting"
//                ) {
//                    ActivityCompat.requestPermissions(
//                        activity,
//                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                        112
//                    )
//                }
//            }
//            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
    }
}

@Composable
fun PermissionComposable(permission: Settings.Permissions.Permission, toggle: (new: Boolean)->Unit) {
    val state by permission.has.collectAsState()
    val isUseless by permission.useless.collectAsState(false)
    Row(
        Modifier
            .clip(CircleShape)
            .rippleClick{
                toggle(!state)
            }
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(permission.name, Modifier.weight(1f), style = TypoStyle(if(!isUseless) FontColor.PRIMARY else FontColor.TERTIARY, FontSize.LARGE))
        Switch(state, {
            toggle(it)
        })
    }
}
@Composable
fun FeatureItem(feature: Settings.Features.Feature, setTo: (Boolean)->Unit) {
    val state by feature.has.collectAsState()
    val isEnablable by combine(flows = feature.reliesOn.map { it.has }){ flowResults ->
        flowResults.all { it }
    }.collectAsState(false)
    Column(
        Modifier
            .padding(15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(feature.name, Modifier.weight(1f), style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE))
            Switch(
                state,
                { setTo(!state) },
                enabled = isEnablable
            )
        }
        Column(
            Modifier.padding(start = 10.dp)
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                feature.reliesOn.forEach {
                    val has by it.has.collectAsState()
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .rippleClick{
                                if(!has) it.set(true)
                            }
                            .background(if(has) Colors.Permissions.GRANTED else Colors.Permissions.REVOKED)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(painterResource(if(has) R.drawable.tick else R.drawable.close), "Needs", Modifier.size(FontSize.SMALL.size.value.dp*.8f), Colors.PRIMARYFONT)
                        Text(it.name, style = TypoStyle(FontColor.PRIMARY, FontSize.SMALL))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(feature.description, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), modifier = Modifier.padding(end = 52.dp))
        }
    }
}

private fun processUsageEvents(
    context: Context,
    start: Long,
    end: Long,
    onSession: (pkg: String, sessionStart: Long, sessionEnd: Long) -> Unit
) {
    val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    val ignoredPackages = setOf(
        "com.samsung.android.incallui",
        "com.sec.android.app.launcher"
    )
    val events = usageStatsManager.queryEvents(start, end)
    val event = UsageEvents.Event()

    var screenOn = false
    var currentForegroundPkg: String? = null
    var lastForegroundTs = 0L

    while (events.hasNextEvent()) {
        if (!events.getNextEvent(event)) break
        val pkg = event.packageName

        when (event.eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                screenOn = true
                continue
            }
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                screenOn = false
                // End current session when screen turns off
                if (currentForegroundPkg != null) {
                    val duration = event.timeStamp - lastForegroundTs
                    if (duration >= 1000) { // At least 1 second
                        onSession(currentForegroundPkg, lastForegroundTs, event.timeStamp)
                    }
                    currentForegroundPkg = null
                }
                continue
            }
        }

        if (pkg != null && pkg in ignoredPackages) {
            if (currentForegroundPkg == pkg) {
                currentForegroundPkg = null
            }
            continue
        }

        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                if (!screenOn) continue
                if (currentForegroundPkg != null) {
                    onSession(currentForegroundPkg, lastForegroundTs, event.timeStamp)
                }
                currentForegroundPkg = pkg
                lastForegroundTs = event.timeStamp
            }
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                if (currentForegroundPkg == pkg) {
                    onSession(currentForegroundPkg, lastForegroundTs, event.timeStamp)
                    currentForegroundPkg = null
                }
            }
        }
    }

    if (currentForegroundPkg != null) {
        onSession(currentForegroundPkg, lastForegroundTs, end)
    }
}

fun getUsageDataBetween(context: Context, start: Long, end: Long): Long {
    var totalForeground = 0L
    processUsageEvents(context, start, end) { _, sessionStart, sessionEnd ->
        totalForeground += max(0L, sessionEnd - sessionStart)
    }
    return totalForeground
}

fun getMappedUsageDataBetween(context: Context, start: Long, end: Long): Map<String, Long> {
    val usageMap = mutableMapOf<String, Long>()
    processUsageEvents(context, start, end) { pkg, sessionStart, sessionEnd ->
        usageMap[pkg] = usageMap.getOrDefault(pkg, 0L) + max(0L, sessionEnd - sessionStart)
    }
    return usageMap
}

fun getUsageDataSessions(context: Context, start: Long, end: Long): List<UsageDataSession> {
    val sessions = mutableListOf<UsageDataSession>()
    processUsageEvents(context, start, end) { pkg, sessionStart, sessionEnd ->
        sessions.add(UsageDataSession(pkg, sessionStart, sessionEnd))
    }
    return sessions.sortedBy { it.start }
}
data class UsageDataSession(val pkg: String, val start: Long, val end: Long)