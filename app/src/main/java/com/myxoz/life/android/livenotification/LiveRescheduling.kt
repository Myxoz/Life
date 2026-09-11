package com.myxoz.life.android.livenotification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.myxoz.life.MainActivity
import com.myxoz.life.R
import com.myxoz.life.android.MainApplication
import com.myxoz.life.android.sensors.navigateTo
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.events.HobbyEvent
import com.myxoz.life.events.LearnEvent
import com.myxoz.life.events.RawEvent
import com.myxoz.life.events.SleepEvent
import com.myxoz.life.events.SocialEvent
import com.myxoz.life.events.SpontEvent
import com.myxoz.life.events.TimewasteEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.WorkEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.dbwrapper.events.EventEntity
import com.myxoz.life.storage.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.NavPath
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.utils.formatMsToDuration
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.joinGramatically
import com.myxoz.life.utils.syncToPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min

class LiveRescheduling(val context: Context): CalendarInterface.CalendarHook {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val intent = Intent(context, LiveEventRefreshReceiver::class.java)
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences("live_notification_prefs", MODE_PRIVATE)
    private val calendar = Calendar.getInstance()
    private val debugOffset = 1000L*60*0
    private val nextScheduledTimestamp = MutableStateFlow(prefs.getLong(NEXTSCHEDULINGTIMESTAMPKEY, -1))
        .apply {
            syncToPrefs(serviceScope, prefs, NEXTSCHEDULINGTIMESTAMPKEY, Long::class)
        }
    private val observedEventId = MutableStateFlow(prefs.getStringSet(OBSERVEDEVENTIDKEY, setOf<String>()) ?: setOf<String>())
        .apply {
            syncToPrefs(serviceScope, prefs, OBSERVEDEVENTIDKEY, Set::class)
        }
    init {
        launchRefresh()
    }
    private val dbInterface: DatabaseInterface by lazy {
        (context.applicationContext as MainApplication)
            .dbInterface
    }
    private val eventDetailsDao: ReadEventDetailsDao by lazy {
        dbInterface
            .readSyncableDaos
            .eventDetailsDao
    }
    private suspend fun refresh() {
        val now = System.currentTimeMillis() + debugOffset
        val current = eventDetailsDao.getEventsOverlapping(now, now)
        Log.d(LOGTAG, "Refresh timestamp called")
        if(current.isEmpty()) {
            clearNotification()
            val next = eventDetailsDao.getEventAfter(now)?.trySynced()
            if(next != null) {
                Log.d(LOGTAG, "Found no current, but next event at ${next.raw.start}")
                scheduleAtFor(next.raw.start - debugOffset, listOf(next.id))
            } else {
                Log.d(LOGTAG, "Found no next event")
                cancelScheduled()
            }
        } else {
            val convertedEvents = current.mapNotNull { it.trySynced() }
            if(convertedEvents.isNotEmpty()) {
                val primaryEventSynced  = convertedEvents.find { it.raw !is PeopleEvent }
                val primaryEvent = primaryEventSynced?.raw
                val primaryId = primaryEventSynced?.id
                val secondaryEventSynced = convertedEvents.find { it.raw is PeopleEvent }
                val secondaryEvent: RawEvent? = secondaryEventSynced?.raw
                val secondaryId = secondaryEventSynced?.id
                if(primaryEvent != null && primaryId != null && secondaryEvent != null && secondaryId != null) {
                    Log.d(LOGTAG, "Found current event ${primaryEvent.toJson()} with and secondary ${secondaryEvent.toJson()}")
                    updateNotification(primaryEvent, secondaryEvent as? PeopleEvent, primaryId)
                    scheduleAtFor(min(primaryEvent.end, secondaryEvent.end) - debugOffset, listOf(primaryId, secondaryId))
                } else {
                    val event = (primaryEvent ?: secondaryEvent) ?: return
                    val id = (primaryId ?: secondaryId) ?: return
                    Log.d(LOGTAG, "Found current event ${event.toJson()}")
                    updateNotification(event, null, id)
                    scheduleAtFor(event.end - debugOffset, listOf(id))
                }
            } else {
                Log.d(LOGTAG, "Found current event but coulnt convert (really odd)")
                cancelScheduled()
            }
        }
    }

    fun cancelScheduled(){
        alarmManager.cancel(getPendingIntent())
        nextScheduledTimestamp.update { -1 }
        observedEventId.update { setOf() }
    }

    fun scheduleAtFor(triggerAt: Long, nextEvent: List<Long>) {
        cancelScheduled()

        observedEventId.update { nextEvent.map { it.toString() }.toSet() }
        nextScheduledTimestamp.update { triggerAt }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if(!alarmManager.canScheduleExactAlarms()) return
        }
        nextScheduledTimestamp.update { triggerAt }
        Log.d(LOGTAG, "Triggering at: "+triggerAt.formatTimeStamp(calendar))
        alarmManager.setExact(
            AlarmManager.RTC,
            triggerAt,
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
        .sortedByDescending { it.durationMs }
        .map { it.durationMs.formatMsToDuration(true) + " " + it.type.displayName }
        .joinGramatically()
    private fun <T: TagLike> mostUsedIcon(tagLikeContainers: List<TimedTagLikeContainer<T>>) = tagLikeContainers
        .maxByOrNull { it.durationMs }?.type
    private fun getPeopleFlowFor(event: PeopleEvent) =
        dbInterface.peopleInterface.getPeople(event.people).map {
            val peopleList = it.map { person -> person.name }
            val people = if((event as? SocialEvent)?.more == true) peopleList.toMutableList().apply { add("Weitere") } else peopleList
            return@map people.joinGramatically()
        }
    fun updateNotification(mainEvent: RawEvent, secondaryEvent: PeopleEvent?, launchEventId: Long) {
        val chipText = when(mainEvent) {
            is SleepEvent -> {
                mainEvent.length().formatMsToDuration(true)
            }
            is TravelEvent -> "Reise"
            is TimewasteEvent, is WorkEvent, is DigSocEvent, is SocialEvent, is LearnEvent, is SpontEvent, is HobbyEvent -> mainEvent.title.takeIf { it.isNotBlank() } ?: mainEvent.type.calendarName
            is EmptyEvent -> "DEBUG"
        }
        val primaryTitle = when(mainEvent) {
            is DigSocEvent -> chipText.takeIf { it.isNotBlank() } ?: mainEvent.digSocEntries.map { it.type.displayName }.joinGramatically()
            is SleepEvent -> "$chipText Schlaf"
            is SpontEvent, is SocialEvent, is HobbyEvent -> chipText
            is LearnEvent -> "Lernen für "+mainEvent.title
            is TimewasteEvent -> (if(mainEvent.title.isNotBlank()) mainEvent.title+" auf " else "")+mainEvent.timewastePlatforms.map { it.type.displayName }.joinGramatically()
            is TravelEvent -> "Nach"
            is WorkEvent -> "Arbeit für $chipText"
            is EmptyEvent -> "DEBUG"
        }
        val color = if(secondaryEvent != null)
            ((secondaryEvent as? RawEvent)?.type ?: EventType.Empty).colors.bg.copy(.2f).compositeOver(mainEvent.type.colors.bg)
        else
            mainEvent.type.colors.bg
        val chipIcon = when(mainEvent) {
            is DigSocEvent -> mostUsedIcon(mainEvent.digSocEntries)?.drawable
            is EmptyEvent -> null
            is WorkEvent, is SpontEvent, is SocialEvent, is LearnEvent, is HobbyEvent -> mainEvent.eventTags.firstOrNull()?.drawable
            is SleepEvent -> R.drawable.sleep
            is TimewasteEvent -> mostUsedIcon(mainEvent.timewastePlatforms)?.drawable
            is TravelEvent -> mostUsedIcon(mainEvent.vehicles)?.drawable
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
        val durationText = "${if (mainEvent.uss) "ca. " else ""}${mainEvent.start.formatDayTime(calendar)} bis ${if(mainEvent.usl) "ca. " else ""}${mainEvent.end.formatDayTime(calendar)}"
        val secondarySubtitle = when(mainEvent) {
            is DigSocEvent -> formatSubtitle(mainEvent.digSocEntries)
            is EmptyEvent -> ""
            is WorkEvent, is LearnEvent, is HobbyEvent -> mainEvent.details ?: mainEvent.eventTags.map { it.displayName }.joinGramatically()
            is SleepEvent -> mainEvent.length().formatMsToDuration(true) + " von "+durationText
            is SocialEvent -> "mit "+(if(mainEvent.more) "Weiteren" else "")
            is SpontEvent -> mainEvent.eventTags.map { it.displayName }.joinGramatically()
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
                    dbInterface.locationInterface.getLocationById(mainEvent.to).collect {
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
            Color.White else Color.Black

    private fun display(constructedNotificatiton: LifeReschedulingNotificationContent) {
        // →
        val icon = constructedNotificatiton.secondaryIcon?.let{ Icon.createWithResource(context, it).setTint(getDarkModeAdjustedColor().toArgb()) }
        val drawable = ContextCompat.getDrawable(context, R.drawable.edit)!!
        val bitmap = createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1)
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

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

        val pendingIntentIntent = Intent(context, MainActivity::class.java).apply {
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
            .setLargeIcon(bitmap)
            .setContentIntent(pendingIntent)
            .setSmallIcon(constructedNotificatiton.chipIcon ?: R.drawable.app) // Chip icon
            .setColor(constructedNotificatiton.color.toArgb()) // Background color on lockscreen
            .setContentTitle("Suck")
            .setContentText("nothing")
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

    private suspend fun EventEntity.trySynced(): SyncedEvent?{
        return SyncedEvent.from(
            RawEvent.PreparedEventContent.prepareContentFor(id, eventDetailsDao)
                ?: return null
        )
    }

    override fun eventUpdated(
        old: SyncedEvent?,
        new: SyncedEvent
    ) {
        if(observedEventId.value.contains(old?.id.toString()) || observedEventId.value.contains(new.id.toString()) || new.raw.start < nextScheduledTimestamp.value) launchRefresh()
    }

    override fun eventRemoved(old: SyncedEvent) {
        if(observedEventId.value.contains(old.id.toString())) launchRefresh()
    }

    override fun eventAdded(new: SyncedEvent) {
        if(new.raw.start < nextScheduledTimestamp.value) launchRefresh()
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
        private const val NEXTSCHEDULINGTIMESTAMPKEY = "next_scheduled_ts"
        private const val OBSERVEDEVENTIDKEY = "observed_event_id"
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