package com.myxoz.life.storage.api.extensions

import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.convertion.events.EventsFromJSON.getRawEventFromJSON
import com.myxoz.life.storage.convertion.events.EventsToJSON.eventToJSONWithType
import com.myxoz.life.storage.events.RawEvent
import java.time.Instant
import java.time.ZoneId

data class RepeatingEventsExtendable(
    val events: List<RepeatingEvent>
): ExtensionSyncable.Extendable {
    override fun specificsToString(): String =
        LifeJSONArray.from(events.map { it.toJSON() }).asString()
    sealed class RepeatType {
        abstract fun getNext(ts: Instant): Instant
        abstract fun toJSON(): LifeJSONObject
        class Daily(
            val nDays: Int
        ): RepeatType() {
            override fun getNext(ts: Instant): Instant =
                ts.atZone(ZoneId.systemDefault()).apply { plusDays(nDays.toLong()) }.toInstant()

            override fun toJSON() = LifeJSONObject()
                .put("nDays", nDays)
        }
        class Weekly(
            val nWeeks: Int
        ): RepeatType() {
            override fun getNext(ts: Instant): Instant =
                ts.atZone(ZoneId.systemDefault()).apply { plusWeeks(nWeeks.toLong()) }.toInstant()

            override fun toJSON() = LifeJSONObject()
                .put("nWeeks", nWeeks)
        }
        class Monthly(
            val dayOfMonth: Int
        ): RepeatType() {
            override fun getNext(ts: Instant): Instant {
                var date = ts.atZone(ZoneId.systemDefault()).plusMonths(1)
                while (date.dayOfMonth != dayOfMonth) { date = date.plusMonths(1) }
                return date.toInstant()
            }

            override fun toJSON(): LifeJSONObject = LifeJSONObject()
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
        fun toJSON(): LifeJSONObject = LifeJSONObject()
            .put("id", id)
            .put("repeat", repeat.toJSON())
            .put("event", event.eventToJSONWithType())
            .put("associated", LifeJSONArray.fromLongs(associatedEvents))

        internal fun next(newEventId: Long?): RepeatingEvent {
            return RepeatingEvent(
                id,
                repeat,
                event.copyWithTimeline(
                    event.eventTime.copy(
                        start = repeat.getNext(event.eventTime.start),
                        end = repeat.getNext(event.eventTime.end)
                    )
                ),
                associatedEvents + listOfNotNull(newEventId)
            )
        }

        companion object {
            fun asRepeatingEvent(obj: LifeJSONObject): RepeatingEvent? {
                val repeatType = obj.getJSONObject("repeat") ?: return null
                val weeks = repeatType.getIntOrNull("nWeeks")
                val days = repeatType.getIntOrNull("nDays")
                return RepeatingEvent(
                    obj.getId(),
                    if(weeks != null)
                        RepeatType.Weekly(weeks)
                    else if(days != null)
                        RepeatType.Daily(days)
                    else
                        RepeatType.Monthly(repeatType.getInt("dayOfMonth"))
                    ,
                    obj.getJSONObject("event").getRawEventFromJSON(),
                    obj.getJSONArray("associated").mapIndexed { getLong(it) }
                )
            }
        }
    }
    companion object: ExtensionSyncable.ExtendableCompanion<RepeatingEventsExtendable> {
        override fun fromString(data: String): RepeatingEventsExtendable =
            RepeatingEventsExtendable(
                LifeJSONArray.safeFrom(data)?.mapJSONObjectsNotNull {
                    RepeatingEvent.asRepeatingEvent(it)
                } ?: error("Failed to read raw event json in repeating events")
            )
    }
}