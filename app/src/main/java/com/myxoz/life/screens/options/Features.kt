package com.myxoz.life.screens.options

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.myxoz.life.dbwrapper.days.DayScreenTimeEntity
import kotlin.math.max

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
    run { // Determines wether the screen was on before the window
        val preEvents = usageStatsManager.queryEvents(start - 24*3600*1000L, start)
        val e = UsageEvents.Event()
        while (preEvents.hasNextEvent()) {
            preEvents.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> screenOn = true
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> screenOn = false
            }
        }
    }
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

fun getMappedUsageDataBetween(context: Context, start: Long, end: Long): List<DayScreenTimeEntity> {
    val usageMap = mutableMapOf<String, Long>()
    processUsageEvents(context, start, end) { pkg, sessionStart, sessionEnd ->
        usageMap[pkg] = usageMap.getOrDefault(pkg, 0L) + max(0L, sessionEnd - sessionStart)
    }
    return usageMap.map { DayScreenTimeEntity(0L, it.key, it.value) }
}

fun getUsageDataSessions(context: Context, start: Long, end: Long): List<UsageDataSession> {
    val sessions = mutableListOf<UsageDataSession>()
    processUsageEvents(context, start, end) { pkg, sessionStart, sessionEnd ->
        sessions.add(UsageDataSession(pkg, sessionStart, sessionEnd))
    }
    return sessions.sortedBy { it.start }
}
data class UsageDataSession(val pkg: String, val start: Long, val end: Long)