package com.myxoz.life.storage.convertion.events

import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.syncables.SyncableIds.getEventCalendarId
import com.myxoz.life.storage.events.DetailsEvent
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.PeopleEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.TagEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TitleEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.additionals.DigSocPlatform
import com.myxoz.life.storage.events.additionals.EventTag
import com.myxoz.life.storage.events.additionals.TagLike
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.TimewastePlatform
import com.myxoz.life.storage.events.additionals.Vehicle

object EventsToJSON {
    fun RawEvent.eventToJSON(json: LifeJSONObject): LifeJSONObject {
        json.put("start", eventTime.start)
        json.put("end", eventTime.end)
        json.put("uss", eventTime.unspecifiedStart)
        json.put("usl", eventTime.unspecifiedLength)
        if(this is TagEvent) json.put("tags", LifeJSONArray.fromInts(eventTags.map { it.id }))
        if(this is TitleEvent) json.put("title", title)
        if(this is DetailsEvent) json.put("details", details)
        if(this is PeopleEvent) json.put("people", LifeJSONArray.fromLongs(people))
        if(this is SocialEvent) json.put("more", more)
        if(this is TimewasteEvent) json.putTimedTaglike("mapping", timewastePlatforms)
        if(this is DigSocEvent) json.putTimedTaglike("mapping", digSocEntries)
        if(this is TravelEvent) json.put("from", from).put("to", to)
            .putTimedTaglike("vehicles", vehicles)
        return json
    }
    fun RawEvent.eventToJSONWithType() = eventToJSON(
        LifeJSONObject().put("type", getEventCalendarId())
    )
    fun <T: TagLike> LifeJSONObject.putTimedTaglike(key: String, values: List<TimedTagLikeContainer<T>>): LifeJSONObject =
        put(key, LifeJSONArray.from(values.map { LifeJSONObject().put("type", when(it.type){
            is DigSocPlatform -> it.type.id
            is EventTag -> it.type.id
            is TimewastePlatform -> it.type.id
            is Vehicle -> it.type.id
        }).put("duration_ms", it.duration) }))
}