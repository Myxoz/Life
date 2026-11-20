package com.myxoz.life.background.livenotification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.myxoz.life.background.navigation.navigateTo
import com.myxoz.life.core.min
import com.myxoz.life.core.minus
import com.myxoz.life.core.plus
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifecore.R
import com.myxoz.life.presentation.DigSocPlatformFormatting.Companion.formattable
import com.myxoz.life.presentation.EventTagFormatting.Companion.formattable
import com.myxoz.life.presentation.TimewastePlatformFormatting.Companion.formattable
import com.myxoz.life.presentation.VehicleFormatting.Companion.formattable
import com.myxoz.life.presentation.asFormattable
import com.myxoz.life.presentation.events.FormattableEvent.Companion.formattable
import com.myxoz.life.presentation.time.formatDayTime
import com.myxoz.life.presentation.time.formatMsToDuration
import com.myxoz.life.presentation.time.joinGramatically
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent
import com.myxoz.life.storage.events.additionals.TagLike
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.aggregator.LifeReschedulingAggregator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes

class LiveRescheduling(
    private val context: Context,
    private val lifeReschedulingAggregator: LifeReschedulingAggregator,
    private val navigationIntent: Intent,
): CalendarInterface.CalendarHook {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val intent = Intent(context, LiveEventRefreshReceiver::class.java)
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val debugOffset = 0.minutes
    private val zone = ZoneId.systemDefault()
    init {
        lifeReschedulingAggregator.subscribeAsHook(this)
        launchRefresh()
    }
    private suspend fun refresh() {
        val now = Instant.now() + debugOffset
        val current = lifeReschedulingAggregator.getEventAt(now)
        Log.d(LOGTAG, "Refresh timestamp called")
        if(current.isEmpty()) {
            clearNotification()
            val next = lifeReschedulingAggregator.getNextEvent(now)
            if(next != null) {
                Log.d(LOGTAG, "Found no current, but next event at ${next.raw.eventTime.start}")
                scheduleAtFor(next.raw.eventTime.start - debugOffset, listOf(next.id))
            } else {
                Log.d(LOGTAG, "Found no next event")
                cancelScheduled()
            }
        } else {
            val primaryEventSynced  = current.find { it.raw !is PeopleEvent }
            val primaryEvent = primaryEventSynced?.raw
            val primaryId = primaryEventSynced?.id
            val secondaryEventSynced = current.find { it.raw is PeopleEvent }
            val secondaryEvent: RawEvent? = secondaryEventSynced?.raw
            val secondaryId = secondaryEventSynced?.id
            if(primaryEvent != null && primaryId != null && secondaryEvent != null && secondaryId != null) {
                Log.d(LOGTAG, "Found current event $primaryEvent with and secondary $secondaryEvent")
                updateNotification(primaryEvent, secondaryEvent as? PeopleEvent, primaryId)
                scheduleAtFor(min(primaryEvent.eventTime.end, secondaryEvent.eventTime.end) - debugOffset, listOf(primaryId, secondaryId))
            } else {
                val event = (primaryEvent ?: secondaryEvent) ?: return
                val id = (primaryId ?: secondaryId) ?: return
                Log.d(LOGTAG, "Found current event $event")
                updateNotification(event, null, id)
                scheduleAtFor(event.eventTime.end - debugOffset, listOf(id))
            }
        }
    }

    fun cancelScheduled(){
        alarmManager.cancel(getPendingIntent())
        lifeReschedulingAggregator.nextScheduledTimestamp = Instant.MIN
        lifeReschedulingAggregator.observedEventIds = listOf()
    }

    fun scheduleAtFor(triggerAt: Instant, nextEvent: List<Long>) {
        cancelScheduled()

        lifeReschedulingAggregator.observedEventIds = nextEvent.map { it.toString() }
        lifeReschedulingAggregator.nextScheduledTimestamp = triggerAt
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!alarmManager.canScheduleExactAlarms()) return
        }
        Log.d(LOGTAG, "Triggering at: $triggerAt")
        alarmManager.setExact(
            AlarmManager.RTC,
            triggerAt.toEpochMilli(),
            getPendingIntent()
        )
    }
    val notificationJobScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var currentNotificationJob: Job? = null
    private fun launchOrClearNotificationJob(newJob: Job?) {
        currentNotificationJob?.cancel()
        currentNotificationJob = newJob
    }
    private data class LifeReschedulingNotificationContent(
        val launchEventId: Long,
        val chipText: String,
        val color: Color,
        val chipIcon: Int?,
        val primaryTitle: String,
        val primarySubTitle: String,
        val secondaryTitle: String,
        val secondarySubtitle: String,
        val secondaryIcon: Int?
    )
    private fun <T: TagLike> formatSubtitle(tagLikeContainers: List<TimedTagLikeContainer<T>>) = tagLikeContainers
        .sortedByDescending { it.duration }
        .map {
            it.duration.formatMsToDuration(true) + " " + it.type.asFormattable().displayName
        }
        .joinGramatically()
    private fun <T: TagLike> mostUsedIcon(tagLikeContainers: List<TimedTagLikeContainer<T>>) = tagLikeContainers
        .maxByOrNull { it.duration }?.type
    private fun getPeopleFlowFor(event: PeopleEvent) =
        lifeReschedulingAggregator.getPeople(event.people).map {
            val peopleList = it.map { person -> person.name }
            val people = if((event as? SocialEvent)?.more == true) peopleList.toMutableList().apply {
                add("Weitere")
            } else peopleList
            return@map people.joinGramatically()
        }
    fun updateNotification(mainEvent: RawEvent, secondaryEvent: PeopleEvent?, launchEventId: Long) {
        val chipText = when(mainEvent) {
            is SleepEvent -> {
                mainEvent.eventTime.length().formatMsToDuration(true)
            }
            is TravelEvent -> "Reise"
            is TimewasteEvent, is WorkEvent, is DigSocEvent, is SocialEvent, is LearnEvent, is SpontEvent, is HobbyEvent -> mainEvent.title.takeIf { it.isNotBlank() }
                ?: mainEvent.formattable().displayName
            is EmptyEvent -> "DEBUG"
        }
        val primaryTitle = when(mainEvent) {
            is DigSocEvent -> chipText.takeIf { it.isNotBlank() }
                ?: mainEvent.digSocEntries.map { it.type.asFormattable().displayName }.joinGramatically()
            is SleepEvent -> "$chipText Schlaf"
            is SpontEvent, is SocialEvent, is HobbyEvent -> chipText
            is LearnEvent -> "Lernen für "+mainEvent.title
            is TimewasteEvent -> (if(mainEvent.title.isNotBlank()) mainEvent.title+" auf " else "") +
                    mainEvent.timewastePlatforms.map { it.type.asFormattable().displayName }.joinGramatically()
            is TravelEvent -> "Nach"
            is WorkEvent -> "Arbeit für $chipText"
            is EmptyEvent -> "DEBUG"
        }
        val color = secondaryEvent?.formattable()?.bg?.copy(.2f)?.compositeOver(mainEvent.formattable().bg)
            ?: mainEvent.formattable().bg
        val chipIcon = when(mainEvent) {
            is DigSocEvent -> mostUsedIcon(mainEvent.digSocEntries)?.formattable()?.drawable
            is EmptyEvent -> null
            is WorkEvent, is SpontEvent, is SocialEvent, is LearnEvent, is HobbyEvent -> mainEvent.eventTags.firstOrNull()?.formattable()?.drawable
            is SleepEvent -> com.myxoz.life.presentation.R.drawable.sleep
            is TimewasteEvent -> mostUsedIcon(mainEvent.timewastePlatforms)?.formattable()?.drawable
            is TravelEvent -> mostUsedIcon(mainEvent.vehicles)?.formattable()?.drawable
        }
        val secondaryIcon = when(mainEvent) {
            is SleepEvent, is TimewasteEvent, is DigSocEvent, is TravelEvent, is WorkEvent, is SpontEvent, is SocialEvent, is LearnEvent, is HobbyEvent -> chipIcon
            is EmptyEvent -> null
        }
        val secondaryTitle = when(mainEvent) {
            is WorkEvent, is TimewasteEvent, is SpontEvent, is SocialEvent, is LearnEvent, is HobbyEvent, is DigSocEvent -> mainEvent.title
            is EmptyEvent -> "Debug"
            is SleepEvent -> "Schlaf"
            is TravelEvent -> "Nach"
        }
        val times = mainEvent.eventTime
        val durationText = "${if (times.unspecifiedStart) "ca. " else ""}${times.start.formatDayTime(zone)} " +
                "bis ${if(times.unspecifiedLength) "ca. " else ""}${times.end.formatDayTime(zone)}"
        val secondarySubtitle = when(mainEvent) {
            is DigSocEvent -> formatSubtitle(mainEvent.digSocEntries)
            is EmptyEvent -> ""
            is WorkEvent, is LearnEvent, is HobbyEvent -> mainEvent.details ?: mainEvent.eventTags.map { it.formattable().displayName }.joinGramatically()
            is SleepEvent -> mainEvent.eventTime.length().formatMsToDuration(true) + " von "+durationText
            is SocialEvent -> "mit "+(if(mainEvent.more) "Weiteren" else "")
            is SpontEvent -> mainEvent.eventTags.map { it.formattable().displayName }.joinGramatically()
            is TimewasteEvent -> formatSubtitle(mainEvent.timewastePlatforms)
            is TravelEvent -> formatSubtitle(mainEvent.vehicles)
        }
        var constructedNotification = LifeReschedulingNotificationContent(
            launchEventId,
            chipText,
            color,
            chipIcon,
            primaryTitle,
            durationText,
            secondaryTitle,
            secondarySubtitle,
            secondaryIcon
        )
        display(constructedNotification)
        launchOrClearNotificationJob(notificationJobScope.launch {
            if(secondaryEvent != null) {
                launch {
                    getPeopleFlowFor(secondaryEvent).collect {
                        constructedNotification = constructedNotification.copy(secondarySubtitle = "$secondarySubtitle mit $it")
                        display(constructedNotification)
                    }
                }
            }
            when(mainEvent) {
                is DigSocEvent -> launch {
                    getPeopleFlowFor(mainEvent).collect {
                        constructedNotification = constructedNotification.copy(secondarySubtitle = "$secondarySubtitle mit $it")
                        display(constructedNotification)
                    }
                }
                is SocialEvent -> launch {
                    getPeopleFlowFor(mainEvent).collect {
                        constructedNotification = constructedNotification.copy(secondarySubtitle = "mit $it")
                        display(constructedNotification)
                    }
                }
                is TravelEvent -> launch {
                    lifeReschedulingAggregator.getLocationById(0L).collect {
                        val location = "Nach "+(it?.name ?: "Ort")
                        constructedNotification = constructedNotification.copy(primaryTitle = location, secondaryTitle = location)
                        display(constructedNotification)
                    }
                }
                else -> {}
            }
        })
    }
    private fun getDarkModeAdjustedColor() =
        if((context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            android.graphics.Color.WHITE else android.graphics.Color.BLACK

    private fun display(constructedNotificatiton: LifeReschedulingNotificationContent) {
        // →
        val icon = constructedNotificatiton.secondaryIcon?.let{ Icon.createWithResource(context, it).setTint(getDarkModeAdjustedColor()) }
        val extras = Bundle().apply {
            putInt("android.ongoingActivityNoti.style", 1)

            // Icon at the end of the line
            // putParcelable("android.ongoingActivityNoti.secondIcon", Icon.createWithBitmap(bitmap))

            // Chip
            putInt("android.ongoingActivityNoti.chipBgColor", constructedNotificatiton.color.toArgb())
            putString("android.ongoingActivityNoti.chipExpandedText", constructedNotificatiton.chipText)

            // Primary layer
            putString("android.ongoingActivityNoti.nowbarPrimaryInfo", constructedNotificatiton.primaryTitle)
            putString("android.ongoingActivityNoti.nowbarSecondaryInfo", constructedNotificatiton.primarySubTitle)

            // Secondary Layer
            putString("android.ongoingActivityNoti.primaryInfo", constructedNotificatiton.secondaryTitle)
            // Icon before the text
            if(icon != null) putParcelable("android.ongoingActivityNoti.secondaryInfoIcon", icon)
            putString("android.ongoingActivityNoti.secondaryInfo", constructedNotificatiton.secondarySubtitle)
        }

        val pendingIntentIntent = navigationIntent.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            navigateTo(NavPath.FULLSCREEN_EVENT.with(constructedNotificatiton.launchEventId))
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, pendingIntentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            // .setLargeIcon(bitmap) maybe change
            .setContentIntent(pendingIntent)
            .setSmallIcon(constructedNotificatiton.chipIcon ?: R.drawable.app) // Chip icon
            .setColor(constructedNotificatiton.color.toArgb()) // Background color on lockscreen
            .setContentTitle(constructedNotificatiton.primaryTitle) // Title in Samsung notification history
            .setContentText(constructedNotificatiton.primarySubTitle)
            // .setWhen(event.start) TODO Check if causes problems
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShowWhen(true)
            .setExtras(extras)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            Log.d(
                LOGTAG,
                "promotable=${notification.hasPromotableCharacteristics()}"
            )
        }
        if (Build.VERSION.SDK_INT >= 36) {
            val manager = context.getSystemService(NotificationManager::class.java)

            Log.d(
                "LiveNotification",
                "canPostPromoted=${manager.canPostPromotedNotifications()}"
            )
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val intent = Intent("com.kakao.taxi.action.POST").apply {
            setClassName("com.kakao.taxi", "com.kakao.taxi.ProxyReceiver")
            putExtra("com.kakao.taxi.extra.NOTIFICATION", notification)
            putExtra("com.kakao.taxi.extra.NOTIFICATION_ID", NOTIFICATION_ID)
        }
        context.sendBroadcast(intent)
    }

    private fun clearNotification() {
        launchOrClearNotificationJob(null)
        val intent = Intent("com.kakao.taxi.action.CANCEL").apply {
            setClassName("com.kakao.taxi", "com.kakao.taxi.ProxyReceiver")
            putExtra("com.kakao.taxi.extra.NOTIFICATION_ID", NOTIFICATION_ID)
        }
        context.sendBroadcast(intent)
    }

    fun getPendingIntent(): PendingIntent{
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun eventUpdated(
        old: SyncedEvent?,
        new: SyncedEvent
    ) {
        val cp = lifeReschedulingAggregator.observedEventIds ?: return
        if(cp.contains(old?.id.toString()) || cp.contains(new.id.toString()) || new.raw.eventTime.start < lifeReschedulingAggregator.nextScheduledTimestamp) launchRefresh()
    }

    override fun eventRemoved(old: SyncedEvent) {
        if(lifeReschedulingAggregator.observedEventIds?.contains(old.id.toString()) == true) launchRefresh()
    }

    override fun eventAdded(new: SyncedEvent) {
        if(new.raw.eventTime.start < lifeReschedulingAggregator.nextScheduledTimestamp) launchRefresh()
    }
    fun launchRefresh(){
        serviceScope.launch {
            refresh()
        }
    }
    companion object {
        const val CHANNEL_ID = "current_event"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE = 2001
        const val LOGTAG = "LiveScheduling"
        fun createNotificationChannel(notificationManager: NotificationManager) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Current event",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the event currently in progress"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}