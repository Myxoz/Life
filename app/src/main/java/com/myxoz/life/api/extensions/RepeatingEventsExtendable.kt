package com.myxoz.life.api.extensions

import android.icu.util.Calendar
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.events.RawEvent
import com.myxoz.life.ui.feed.fullscreenevent.getId
import com.myxoz.life.utils.asList
import com.myxoz.life.utils.asNotNullList
import com.myxoz.life.utils.getJSONObjectOrNull
import com.myxoz.life.utils.getSafeLong
import com.myxoz.life.utils.jsonArray
import org.json.JSONArray
import org.json.JSONObject

data class RepeatingEventsExtendable(
    val events: List<RepeatingEvent>
): ExtensionSyncable.Extendable {
    override fun specificsToString(): String =
        events.jsonArray { it.toJSON() }.toString()
    sealed class RepeatType {
        abstract fun getNext(calendar: Calendar, ts: Long): Long
        abstract fun toJSON(): JSONObject
        class Daily(
            val nDays: Int
        ): RepeatType() {
            override fun getNext(
                calendar: Calendar,
                ts: Long
            ): Long {
                calendar.timeInMillis = ts
                calendar.add(Calendar.DATE, nDays)
                return calendar.timeInMillis
            }

            override fun toJSON(): JSONObject = JSONObject()
                .put("nDays", nDays)
        }
        class Weekly(
            val nWeeks: Int
        ): RepeatType() {
            override fun getNext(
                calendar: Calendar,
                ts: Long
            ): Long {
                calendar.timeInMillis = ts
                calendar.add(Calendar.WEEK_OF_MONTH, nWeeks)
                return calendar.timeInMillis
            }

            override fun toJSON(): JSONObject = JSONObject()
                .put("nWeeks", nWeeks)
        }
        class Monthly(
            val dayOfMonth: Int
        ): RepeatType() {
            override fun getNext(
                calendar: Calendar,
                ts: Long
            ): Long {
                calendar.timeInMillis = ts
                val date = calendar.get(Calendar.DAY_OF_MONTH)
                do {
                   calendar.add(Calendar.MONTH, 1)
                } while (date != calendar.get(Calendar.DAY_OF_MONTH))
                return calendar.timeInMillis
            }

            override fun toJSON(): JSONObject = JSONObject()
                .put("dayOfMonth", dayOfMonth)
        }
        /** IMP!! When adding something new add it to [RepeatingEvent.asRepeatingEvent] */
    }
    data class RepeatingEvent(
        val id: Long,
        val repeat: RepeatType,
        val event: RawEvent,
        val associatedEvents: List<Long>,
    ) {
        fun toJSON(): JSONObject = JSONObject()
            .put("id", id.toString())
            .put("repeat", repeat.toJSON())
            .put("event", event.toJson())
            .put("associated", associatedEvents.jsonArray { it.toString() })

        fun next(newEventId: Long?): RepeatingEvent {
            val calendar = Calendar.getInstance()
            return RepeatingEvent(
                id,
                repeat,
                event.copyWithTimes(
                    repeat.getNext(calendar, event.start),
                    repeat.getNext(calendar, event.end)
                ),
                associatedEvents + listOfNotNull(newEventId)
            )
        }

        companion object {
            fun asRepeatingEvent(obj: JSONObject): RepeatingEvent? {
                val repeatType = obj.getJSONObjectOrNull("repeat") ?: return null
                return RepeatingEvent(
                    obj.getId(),
                    if(repeatType.has("nWeeks"))
                        RepeatType.Weekly(repeatType.getInt("nWeeks"))
                    else if(repeatType.has("nDays"))
                        RepeatType.Daily(repeatType.getInt("nDays"))
                    else
                        RepeatType.Monthly(repeatType.getInt("dayOfMonth"))
                    ,
                    RawEvent.fromJSON(obj.getJSONObject("event")),
                    obj.getJSONArray("associated").asList { getSafeLong(it) }
                )
            }
        }
    }
    companion object: ExtensionSyncable.ExtendableCompanion<RepeatingEventsExtendable> {
        override fun fromString(data: String): RepeatingEventsExtendable =
            RepeatingEventsExtendable(
                JSONArray(data).asNotNullList {
                    RepeatingEvent.asRepeatingEvent(getJSONObject(it))
                }
            )
    }
}