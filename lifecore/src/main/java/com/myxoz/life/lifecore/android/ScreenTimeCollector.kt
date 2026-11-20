package com.myxoz.life.lifecore.android

import android.Manifest
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.annotation.RequiresPermission
import com.myxoz.life.core.atEndAsInstant
import com.myxoz.life.core.atStartAsInstant
import com.myxoz.life.core.minus
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ScreenTimeCollector(val context: Context) {
    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    private fun processUsageEvents(
        start: Instant,
        end: Instant,
        onSession: (pkg: String, sessionStart: Long, sessionEnd: Long) -> Unit
    ) {
        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
        val ignoredPackages = setOf(
            "com.samsung.android.incallui",
            "com.sec.android.app.launcher"
        )
        val events = usageStatsManager.queryEvents(start.toEpochMilli(), end.toEpochMilli())
        val event = UsageEvents.Event()

        var screenOn = false
        run { // Determines wether the screen was on before the window
            val preEvents = usageStatsManager.queryEvents((start - 24.hours).toEpochMilli(), start.toEpochMilli())
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
            onSession(currentForegroundPkg, lastForegroundTs, end.toEpochMilli())
        }
    }

    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    fun getTotalForegroundBetween(start: Instant, end: Instant): Long {
        var totalForeground = 0L
        processUsageEvents(start, end) { _, sessionStart, sessionEnd ->
            totalForeground += max(0L, sessionEnd - sessionStart)
        }
        return totalForeground
    }

    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    fun getAccumulatedUsageDurationByPackagename(start: Instant, end: Instant): List<AccUsageDataSession> {
        val usageMap = mutableMapOf<String, Long>()
        processUsageEvents(start, end) { pkg, sessionStart, sessionEnd ->
            usageMap.merge(pkg,  (sessionEnd - sessionStart).coerceAtLeast(0L), Long::plus)
        }
        return usageMap.map { AccUsageDataSession(it.key, it.value.milliseconds) }
    }

    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    fun getRawSessions(start: Instant, end: Instant): List<UsageDataSession> {
        val sessions = mutableListOf<UsageDataSession>()
        processUsageEvents(start, end) { pkg, sessionStart, sessionEnd ->
            sessions.add(UsageDataSession(pkg, sessionStart, sessionEnd))
        }
        return sessions.sortedBy { it.start }
    }

    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    fun getScreenTimeOnAsFlow(
        date: LocalDate,
        zone: ZoneId,
    ): Flow<List<AccUsageDataSession>> = flow {
        while (currentCoroutineContext().isActive) {
            val list = getAccumulatedUsageDurationByPackagename(
                date.atStartAsInstant(zone),
                if(date == LocalDate.now()) Instant.now() else date.atEndAsInstant(zone)
            ).sortedByDescending { it.duration }
            emit(list)
            delay(1.seconds)
        }
    }

    @RequiresPermission(Manifest.permission.PACKAGE_USAGE_STATS)
    fun getTotalScreenTimeOnAsFlow(
        date: LocalDate,
        zone: ZoneId,
        rhythm: Duration = 1.seconds,
    ): Flow<Duration> = flow {
        while (currentCoroutineContext().isActive) {
            val totalScreenTime = getTotalForegroundBetween(
                date.atStartAsInstant(zone),
                if(date == LocalDate.now()) Instant.now() else date.atEndAsInstant(zone)
            )
            emit(totalScreenTime.milliseconds)
            delay(rhythm)
        }
    }
    data class UsageDataSession(val pkg: String, val start: Long, val end: Long)
    data class AccUsageDataSession(val pkg: String, val duration: Duration)
}