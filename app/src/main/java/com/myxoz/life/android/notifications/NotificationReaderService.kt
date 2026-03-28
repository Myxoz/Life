package com.myxoz.life.android.notifications

import android.app.Notification
import android.app.Person
import android.icu.util.Calendar
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.android.autodetect.AutoDetectCall
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.repositories.MainApplication
import com.myxoz.life.utils.SharedPrefsUtils.edit
import com.myxoz.life.utils.asList
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.roundToNearest15Min
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class NotificationReaderService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val repo = (applicationContext as MainApplication).repositories
        val extras = sbn.notification.extras

        when(sbn.packageName) {
            "de.fiduciagad.android.wlwallet" -> {
                if(!Settings.Feature.ReadPaymentNotifications.isEnabled(repo.permissionChecker)) return
                val title = extras.getCharSequence("android.title")?.toString() ?: return
                val text = extras.getCharSequence("android.text")?.toString() ?: return

                val cents = title.substringBefore(" EUR").trim().replace(",","").toInt()

                val datePart = text.substringAfterLast(" am ").substringBefore(" Uhr").split(" um ")
                val sDate = datePart[0].split(".")
                val time = datePart[1].split(":")
                if(datePart.size != 2 || sDate.size != 3 || time.size != 2) {
                    Log.w("Transaction", "A payment notification couldnt be parsed, this is indication that a structural change of the notification happened")
                    return
                }
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.YEAR, sDate[2].toIntOrNull()?:return)
                calendar.set(Calendar.MONTH, sDate[1].toIntOrNull()?.minus(1)?:return)
                calendar.set(Calendar.DAY_OF_MONTH, sDate[0].toIntOrNull()?:return)

                calendar.set(Calendar.HOUR_OF_DAY, time[0].toIntOrNull()?:return)
                calendar.set(Calendar.MINUTE, time[1].toIntOrNull()?:return)
                calendar.set(Calendar.SECOND, 0)
                if(abs(sbn.postTime - calendar.timeInMillis) > 1000*20*3600){
                    // If the notification is 20h appart from beeing sent and the timestamp in the
                    // notification and if the day would be pretty exactly one more. Correct it
                    // I don't know why they can't code but 2 Notifications next to eachother look like this:
                    // 23:48 at 5.2.2026 followed by 00:17 at 5.2.2026. This is wrong so we need to correct
                    // it like this.
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    if(abs(sbn.postTime - calendar.timeInMillis) > 5*1000*60) {
                        // I cant help you either...
                        // Revert
                        calendar.add(Calendar.DAY_OF_MONTH, -1)
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    repo.bankingRepo.putFutureTransaction(
                        -cents,
                        calendar.timeInMillis,
                        false,
                        true,
                        "Unbekannt",
                        null
                    )
                }
            }
            "com.whatsapp" -> {
                if(!Settings.Feature.AutoDetectWhatsAppCalls.isEnabled(repo.permissionChecker)) return
                CoroutineScope(Dispatchers.IO).launch {
                    if(sbn.notification.category != Notification.CATEGORY_CALL) return@launch
                    val allPeople = repo.peopleRepo.getAllPeople().first()
                    repo.prefs.edit {
                        putString("last_whatsapp_call", JSONObject().apply {
                            put("start", sbn.notification.`when`.toString())
                            put("key", sbn.key)
                            put("ids", JSONArray().apply {
                                (extras.get(Notification.EXTRA_PEOPLE_LIST) as? ArrayList<*> ?: return@edit)
                                    .forEach {
                                            person -> if (person !is Person) return@forEach; allPeople.find {
                                         it.matchesWhatsAppNumber((person.uri?.removePrefix("tel:") ?: return@find false))
                                    }?.let { put(it.id) }
                                    }
                                })
                            }.toString()
                        )
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(unreliableSBN: StatusBarNotification?) {
        if(unreliableSBN  == null) return
        val repo = (applicationContext as MainApplication).repositories
        // dumpNotification(unreliableSBN)
        if(!Settings.Feature.AutoDetectWhatsAppCalls.isEnabled(repo.permissionChecker)) return
        if(unreliableSBN.packageName != "com.whatsapp") return
        val old = repo.prefs.getString("last_whatsapp_call", null) ?: return
        val json = JSONObject(old)
        if(json.getString("key") != unreliableSBN.key) return
        /*
        We will be using when and not postTime due to:
        Cite: https://emanual.github.io/Android-docs/reference/android/support/v4/app/NotificationCompat.html

        EXTRA_SHOW_CHRONOMETER
        Notification extras key: whether the when field set using setWhen(long)
        should be shown as a count-up timer (specifically a Chronometer) instead of a timestamp,
        as supplied to setUsesChronometer(boolean).

        ------
        This suggests that the when-field is displayed to the user and therefore more correct
        Example:
        packageName = com.whatsapp
        id = 23
        postTime = 1774218212158
        key = 0|com.whatsapp|23|null|10310
        isOngoing = true
        isClearable = false
        user = UserHandle{0}
        when = 1774218212118
        category = call
        visibility = 0
        priority = 1
        flags = 8290
        number = 0
        EXTRA_TITLE =  Sophia Cools
        EXTRA_TEXT = Ongoing voice call
        EXTRA_TEMPLATE = android.app.Notification$CallStyle
        EXTRA_PROGRESS = 0
        EXTRA_PROGRESS_MAX = 0
        EXTRA_SHOW_CHRONOMETER = true
        actions.count = 1
        action[0].title = Hang Up
        action[0].intent = PendingIntent{183fc67: android.os.BinderProxy@6455d4e}
        extras[android.title] =  Sophia Cools
        extras[android.reduced.images] = true
        extras[video_call] = false
        extras[android.template] = android.app.Notification$CallStyle
        extras[android.showChronometer] = true
        extras[android.people.list] = [0]: Person{uri=tel:[CENSORED], isBot=false, isImportant=false}
        extras[android.text] = Ongoing voice call
        extras[android.progress] = 0
        extras[androidx.core.app.extra.COMPAT_TEMPLATE] = androidx.core.app.NotificationCompat$CallStyle
        extras[android.progressMax] = 0
        extras[android.appInfo] = ApplicationInfo{ac40a14 com.whatsapp}
        extras[android.callIsVideo] = false
        extras[android.hangUpIntent] = PendingIntent{292bd: android.os.BinderProxy@6455d4e}
        extras[android.showWhen] = true
        extras[isGroupCall] = false
        extras[android.callType] = 2
        extras[android.largeIcon] = Icon(typ=BITMAP size=101x101)
        extras[android.callPerson] = Person{name= Sophia Cools, isBot=false, isImportant=false}
        extras[android.progressIndeterminate] = false
        contentIntent = PendingIntent{4fc40b2: android.os.BinderProxy@64aae6f}
        channelId = silent_notifications_6
        */
        CoroutineScope(Dispatchers.IO).launch {
            val start = (json.getLongOrNull("start") ?: return@launch)
            val eventStart = start.roundToNearest15Min()
            val currentTime = System.currentTimeMillis()
            val endTime = currentTime.roundToNearest15Min().coerceAtLeast(eventStart + 15*1000*60)
            var proposed = DigSocEvent(
                eventStart,
                endTime,
                false,
                false,
                listOf(TimedTagLikeContainer(DigSocPlatform.Anruf, currentTime - start)),
                "Anruf",
                json.getJSONArray("ids").asList { getString(it).toLong() },
            )
            val autodetectPrefs = getSharedPreferences(AutoDetect.AUTODETECT_PREFS, MODE_PRIVATE)
            val oldPrefs = (autodetectPrefs.getStringSet(AutoDetect.getStored(AutoDetectCall.SPK), setOf<String>()) ?: setOf<String>()).toMutableSet()
            val max = oldPrefs.maxByOrNull { JSONObject(it).getLong("end") }
            if(max != null) {
                val oldEvent = ProposedEvent.fromJSON(JSONObject(max))
                val merge = oldEvent.type == EventType.DigSoc && oldEvent is DigSocEvent &&
                        oldEvent.digSocEntries.all { old -> proposed.digSocEntries.any { old.type == it.type } } &&
                        oldEvent.title == proposed.title &&
                        oldEvent.people == proposed.people &&
                        proposed.start - oldEvent.end < 7*60*1000L // Less than 7m gap
                if(merge) {
                    proposed = DigSocEvent(
                        oldEvent.start,
                        proposed.end,
                        false,
                        false,
                        listOf(
                            TimedTagLikeContainer(
                                DigSocPlatform.Anruf,
                                oldEvent.digSocEntries.sumOf { it.durationMs } +
                                        proposed.digSocEntries.sumOf { it.durationMs }
                            )
                        ),
                        proposed.title,
                        proposed.people
                    )
                    oldPrefs.remove(max)
                }
            }
            oldPrefs.add(proposed.toJson().toString())
            autodetectPrefs.edit {
                putStringSet(AutoDetect.getStored(AutoDetectCall.SPK), oldPrefs)
            }
        }
    }
    override fun onListenerConnected() {
        super.onListenerConnected()
        // used for debugging reasons when its needed uncomment
        // dumpAllNotifications()
//        for(sbn in activeNotifications) {
//            this.onNotificationPosted(sbn)
//        }
    }
    // AI-Generated
    private fun dumpNotification(sbn: StatusBarNotification){
        val notif = sbn.notification
        val extras = notif.extras

        val present = mutableListOf<String>()
        val nulls = mutableListOf<String>()

        fun add(name: String, value: Any?) {
            if (value != null) {
                present.add("$name = $value")
            } else {
                nulls.add(name)
            }
        }

        // ===== Basic SBN info =====
        add("packageName", sbn.packageName)
        add("id", sbn.id)
        add("tag", sbn.tag)
        add("postTime", sbn.postTime)
        add("key", sbn.key)
        add("isOngoing", sbn.isOngoing)
        add("isClearable", sbn.isClearable)
        add("user", sbn.user)

        // ===== Notification core =====
        add("when", notif.`when`)
        add("category", notif.category)
        add("visibility", notif.visibility)
        add("priority", notif.priority)
        add("flags", notif.flags)
        add("number", notif.number)
        add("tickerText", notif.tickerText)

        // ===== Common extras =====
        add("EXTRA_TITLE", extras.getCharSequence(Notification.EXTRA_TITLE))
        add("EXTRA_TEXT", extras.getCharSequence(Notification.EXTRA_TEXT))
        add("EXTRA_SUB_TEXT", extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        add("EXTRA_BIG_TEXT", extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add("EXTRA_SUMMARY_TEXT", extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
        add("EXTRA_INFO_TEXT", extras.getCharSequence(Notification.EXTRA_INFO_TEXT))

        add("EXTRA_TEMPLATE", extras.getString(Notification.EXTRA_TEMPLATE))
        add("EXTRA_PEOPLE", extras.get(Notification.EXTRA_PEOPLE))
        add("EXTRA_CONVERSATION_TITLE", extras.getCharSequence("android.conversationTitle"))

        // ===== Progress / misc =====
        add("EXTRA_PROGRESS", extras.getInt(Notification.EXTRA_PROGRESS, -1).takeIf { it != -1 })
        add("EXTRA_PROGRESS_MAX", extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1).takeIf { it != -1 })
        add("EXTRA_SHOW_CHRONOMETER", extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false).takeIf { it })

        // ===== Actions =====
        notif.actions?.let { actions ->
            present.add("actions.count = ${actions.size}")

            actions.forEachIndexed { index, action ->
                val prefix = "action[$index]"

                add("$prefix.title", action.title)
                add("$prefix.intent", action.actionIntent)

                val hasRemoteInputs = action.remoteInputs?.isNotEmpty() == true
                add("$prefix.hasRemoteInputs", hasRemoteInputs.takeIf { it })

                action.remoteInputs?.forEachIndexed { riIndex, ri ->
                    add("$prefix.remoteInput[$riIndex].resultKey", ri.resultKey)
                    add("$prefix.remoteInput[$riIndex].label", ri.label)
                }
            }
        } ?: nulls.add("actions")

        // ===== Extras dump (FULL) =====
        if (extras != null) {
            for (key in extras.keySet()) {
                val value = extras.get(key)

                when (value) {
                    is Person -> {
                        add("extras[$key]", dumpPerson(value))
                    }
                    is ArrayList<*> -> {
                        if (value.all { it is android.app.Person }) {
                            val peopleDump = value.mapIndexed { index, p ->
                                "[$index]: ${dumpPerson(p as Person)}"
                            }.joinToString(", ")
                            add("extras[$key]", peopleDump)
                        } else {
                            add("extras[$key]", value)
                        }
                    }
                    else -> add("extras[$key]", value)
                }
            }
        } else {
            nulls.add("extras")
        }

        // ===== Intents =====
        add("contentIntent", notif.contentIntent)
        add("deleteIntent", notif.deleteIntent)
        add("fullScreenIntent", notif.fullScreenIntent)

        // ===== Channels (important for WhatsApp) =====
        add("channelId", notif.channelId)

        // ===== Final log =====
        Log.d("NotificationListener",
            """
            ===== NOTIFICATION START =====
            ${present.joinToString("\n")}
            
            ----- NULL FIELDS -----
            ${nulls.joinToString("\n")}
            ===== NOTIFICATION END =====
            """.trimIndent()
        )
    }
    private fun dumpAllNotifications() {
        val sbns = activeNotifications ?: return
        for (sbn in sbns) {
            dumpNotification(sbn)
        }
    }
    fun dumpPerson(person: Person): String {
        val parts = mutableListOf<String>()

        person.name?.let { parts.add("name=$it") }
        person.uri?.let { parts.add("uri=$it") }
        person.key?.let { parts.add("key=$it") }
        parts.add("isBot=${person.isBot}")
        parts.add("isImportant=${person.isImportant}")

        return "Person{${parts.joinToString(", ")}}"
    }
}
