package com.myxoz.life.storage.convertion.events

import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.syncables.SyncedEvent
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent
import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.EventTime
import com.myxoz.life.storage.events.additionals.TagLike
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.TimewastePlatform
import com.myxoz.life.storage.events.additionals.Vehicle

object EventsFromJSON {
    fun LifeJSONObject.getRawEventFromJSON(): RawEvent {
        val timeLine = getEventTime()
        return when(val type = getType()) {
            EmptyEvent.CALENDARID -> EmptyEvent(timeLine)
            SleepEvent.CALENDARID -> SleepEvent(timeLine)
            SpontEvent.CALENDARID ->
                SpontEvent(timeLine, getTags(), getTitle())
            HobbyEvent.CALENDARID ->
                HobbyEvent(timeLine, getTags(), getTitle(), getDetails())
            LearnEvent.CALENDARID ->
                LearnEvent(timeLine, getTags(), getTitle(), getDetails())
            SocialEvent.CALENDARID ->
                SocialEvent(timeLine, getTags(), getTitle(), getPeople(), getMore())
            TravelEvent.CALENDARID ->
                TravelEvent(timeLine, getFrom(), getTo(), getVehicles())
            DigSocEvent.CALENDARID ->
                DigSocEvent(timeLine, getDigSocEntries(), getTitle(), getPeople())
            WorkEvent.CALENDARID ->
                WorkEvent(timeLine, getTags(), getTitle(), getDetails())
            TimewasteEvent.CALENDARID ->
                TimewasteEvent(timeLine, getTimewastePlatforms(), getTitle())
            else -> throw Error("Tried to construct event with unknown type $type}")
        }
    }
    fun LifeJSONObject.getSyncedEventFromJSON(): SyncedEvent =
        SyncedEvent(
            getId(),
            getInstant("created"),
            getInstant("edited"),
            getRawEventFromJSON()
        )

    private fun LifeJSONObject.getEventTime(): EventTime =
        EventTime(
            getInstant("start"),
            getInstant("end"),
            getBoolean("uss"),
            getBoolean("usl")
        )
    private fun LifeJSONObject.getTitle() = getString("title")
    private fun LifeJSONObject.getDetails() = getStringOrNull("details")
    private fun LifeJSONObject.getPeople() =
        getJSONArray("people").mapIndexed { getLong(it) }
    private fun LifeJSONObject.getMore() = getBoolean("more")
    private fun LifeJSONObject.getFrom() = getLong("from")
    private fun LifeJSONObject.getTo() = getLong("to")
    private fun LifeJSONObject.getTags(): List<EventTag> =
        getJSONArray("tags").mapNotNullIndexed { EventTag.getTagById(getInt(it)) }

    private fun LifeJSONObject.getVehicles(): List<TimedTagLikeContainer<Vehicle>> =
        getJSONArray("vehicles").getTimedTagLikeContainer {
            Vehicle.getById(it)
        }

    private fun LifeJSONObject.getDigSocEntries(): List<TimedTagLikeContainer<DigSocPlatform>> =
        getJSONArray("mapping").getTimedTagLikeContainer {
            DigSocPlatform.getById(it)
        }

    private fun LifeJSONObject.getTimewastePlatforms(): List<TimedTagLikeContainer<TimewastePlatform>> =
        getJSONArray("mapping").getTimedTagLikeContainer {
            TimewastePlatform.getById(it)
        }

    private fun <T: TagLike> LifeJSONArray.getTimedTagLikeContainer(getType: (Int) -> T?): List<TimedTagLikeContainer<T>> =
        mapJSONObjectsNotNull {
            TimedTagLikeContainer(
                getType(it.getType()) ?: return@mapJSONObjectsNotNull null,
                it.getLong("duration_ms")
            )
        }
}