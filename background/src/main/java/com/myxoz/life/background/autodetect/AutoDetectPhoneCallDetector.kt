package com.myxoz.life.background.autodetect

import android.provider.CallLog
import com.myxoz.life.core.plus
import com.myxoz.life.core.roundToNearest15Min
import com.myxoz.life.core.toInstant
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object AutoDetectPhoneCallDetector {
    val SPK = AutoDetect.AutoDetectables.AutoDetectPhoneCalls
    private data class Call(val start: Instant, val duration: Duration, val number: String)
    suspend fun getSessions(helper: AutoDetect.AutoDetectHelper): List<RawEvent>{
        val lookAfter = helper.getLastTimestamp(SPK)
        val resolver = helper.context.contentResolver
        val cursor = resolver.query(
            // Can't throw, permission assured in AutoDetect.kt
            CallLog.Calls.CONTENT_URI,
            null,
            if (lookAfter != null) "${CallLog.Calls.DATE} > ?" else null,
            if (lookAfter != null) arrayOf(lookAfter.toEpochMilli().toString()) else null,
            "${CallLog.Calls.DATE} DESC"
        )
        val allPeople = helper.allPeople
        val allSessions = mutableListOf<Call>()
        cursor?.use {
            while (cursor.moveToNext()){
                try {
                    allSessions.add(
                        Call(
                            it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE)).toInstant(),
                            it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)).milliseconds,
                            it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)),
                        )
                    )
                } catch (_: Exception){}
            }
        }
        val callSessions = allSessions
            // Grouping to filter out composed call (7m normal, 1m video, 20m normal, 28m super) -> 28m super
            .groupBy { it.start }
            .asSequence()
            .map { v ->
                v.value.maxBy { it.duration }
            }
            .sortedBy { it.start }
            .fold(mutableListOf<Call>()) { acc, call ->
                if (acc.isEmpty()) return@fold acc.apply { add(call) }
                val last = acc.last()
                if (call.number == last.number && call.start <= last.start + last.duration + 15.minutes) {
                    acc[acc.size - 1] = last.copy(duration = last.duration + call.duration)
                } else {
                    acc.add(call)
                }
                acc
            }
            .filter {
                it.duration > 8.minutes
            }
            .map {
                val start = it.start
                DigSocEvent(
                    EventTime(
                        start,
                        (start + it.duration).coerceAtLeast(start + 15.minutes).roundToNearest15Min(),
                        unspecifiedStart = false,
                        unspecifiedLength = false
                    ),
                    listOf(
                        TimedTagLikeContainer(
                            DigSocPlatform.Call,
                            it.duration
                        )
                    ),
                    "Anruf",
                    allPeople.mapNotNull { person ->
                        if (PhoneNumberParser.areEqual(
                                person.phoneNumber ?: return@mapNotNull null,
                                it.number
                            )
                        )
                            person.id
                        else
                            null
                    }.toSet()
                )
            }
        allSessions.lastOrNull()?.let {
            helper.putLastTimestamp(SPK, it.start + 1.seconds)
        }
        return callSessions
    }
}