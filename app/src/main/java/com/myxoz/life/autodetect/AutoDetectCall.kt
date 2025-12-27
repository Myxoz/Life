package com.myxoz.life.autodetect

import android.content.Context
import android.provider.CallLog
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.DigSocEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.utils.roundToNearest15Min

object AutoDetectCall {
    const val SPK = "declined_calls"
    private data class Call(val start: Long, val duration: Long, val number: String)
    suspend fun getSessions(context: Context, db: StorageManager): List<ProposedEvent>{
        val resolver = context.contentResolver
        val cursor = resolver.query(
            // Can't throw, permission assured in AutoDetect.kt
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )
        val allPeople = db.people.getAllPeople().map { PersonSyncable.from(db, it) }
        val allSessions = mutableListOf<Call>()
        cursor?.use {
            while (cursor.moveToNext()){
                try {
                    allSessions.add(
                        Call(
                            it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE)),
                            it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)),
                            it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)),
                        )
                    )
                } catch (_: Exception){}
            }
        }
        return allSessions
            // Grouping to filter out composed call (7m normal, 1m video, 20m normal, 28m super) -> 28m super
            .groupBy { it.start }
            .map { v ->
                v.value.maxBy { it.duration }
            }
            .sortedBy { it.start }
            .fold(mutableListOf<Call>()) { acc, call ->
                if (acc.isEmpty()) return@fold acc.apply { add(call) }
                val last = acc.last()
                if (call.number == last.number && call.start <= last.start + last.duration * 1000L + 15 * 60 * 1000L) {
                    acc[acc.size - 1] = last.copy(duration = last.duration + call.duration)
                } else {
                    acc.add(call)
                }
                acc
            }
            .filter {
                it.duration > 60*8 /* Count calls above 8m */
            }
            .map {
                val start = (it.start).roundToNearest15Min()
                DigSocEvent(
                    start,
                    (it.start + it.duration*1000L).roundToNearest15Min().coerceAtLeast(start),
                    false,
                    false,
                    listOf(
                        TimedTagLikeContainer(DigSocPlatform.Anruf, it.duration*1000L)
                    ),
                    "Anruf",
                    listOf(allPeople.find { person -> person.phoneNumber == it.number }?.id ?: -1)
                )
            }
    }
}