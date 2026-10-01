package com.myxoz.life.storage.api.extensions

import com.myxoz.life.core.UnixWeek
import com.myxoz.life.storage.api.LifeJSONArray
import com.myxoz.life.storage.api.LifeJSONObject
import com.myxoz.life.storage.api.syncables.ExtensionSyncable
import com.myxoz.life.storage.events.additionals.EventTag
import java.time.LocalDate

class StreakExtendable(
    val streaks: List<StreakItem>
): ExtensionSyncable.Extendable {
    override fun specificsToString() =
        LifeJSONArray.from(streaks.map { it.toJSON() }).asString()
    companion object: ExtensionSyncable.ExtendableCompanion<StreakExtendable>{
        override fun fromString(data: String): StreakExtendable = StreakExtendable(
            LifeJSONArray.safeFrom(data)?.mapJSONObjects {
                StreakItem.fromJSON(it)
            } ?: error("Cannot parse Streak extendable as JSON")
        )
    }
    enum class Timespan(val displayName: String, val jsonName: String, val plural: String, val now: String){
        Day("Tag", "day", "e", "Heute"),
        Week("Woche", "week", "n", "Diese Woche");
        fun now(): Long{
            return when(this){
                Day -> LocalDate.now().toEpochDay()
                Week -> UnixWeek.of(LocalDate.now()).week
            }
        }
    }
    enum class DayTargetType(val jsonName: String, val displayName: String) {
        Steps("steps", "Schritte"),
        Screentime("screentime", "Bildschirmzeit"),
    }
    sealed class StreakTarget{
        abstract fun specificsToJSON(): LifeJSONObject
        fun toJSON(): LifeJSONObject = specificsToJSON().put("streak_type", if(this is EventTarget) "event" else "day")
        data class EventTarget(
            val searchEventAmount: Boolean,
            val type: Int,
            val tags: List<EventTag>,
            val people: List<Long>,
            val title: String?,
        ): StreakTarget() {
            override fun specificsToJSON() = LifeJSONObject()
                .put("searchEventAmount", searchEventAmount)
                .put("type", type)
                .put("tags", LifeJSONArray.fromInts(tags.map { it.id }))
                .put("people", LifeJSONArray.fromLongs(people))
                .put("title", title)

        }
        class DayTarget(
            val type: DayTargetType,
        ): StreakTarget() {
            override fun specificsToJSON() = LifeJSONObject().put("type", type.jsonName)
        }
        companion object {
            fun fromJSON(json: LifeJSONObject): StreakTarget {
                return if(json.getString("streak_type") == "event") {
                    EventTarget(
                        json.getBoolean("searchEventAmount"),
                        json.getType(),
                        json.getJSONArray("tags").mapNotNullIndexed { EventTag.getTagById(getInt(it)) },
                        json.getJSONArray("people").mapIndexed { getLong(it) },
                        json.getStringOrNull("title"),
                    )
                } else {
                    DayTarget(
                        DayTargetType.entries.first { it.jsonName == json.getString("type") }
                    )
                }
            }
        }
    }
    data class StreakItem(
        val id: Long,
        val atLeast: Boolean,
        val threshold: Long,
        val timespan: Timespan,
        val target: StreakTarget,
        val title: String,
        val smallTitle: String?,
    ) {
        fun toJSON() = LifeJSONObject()
            .put("id", id.toString())
            .put("atLeast", atLeast)
            .put("threshold", threshold.toString())
            .put("timespan", timespan.jsonName)
            .put("target", target.toJSON())
            .put("title", title)
            .put("smallTitle", smallTitle)
        companion object {
            fun fromJSON(json: LifeJSONObject) = StreakItem(
                json.getId(),
                json.getBoolean("atLeast"),
                json.getLong("threshold"),
                if(json.getString("timespan") == Timespan.Day.jsonName) Timespan.Day else Timespan.Week,
                StreakTarget.fromJSON(json.getJSONObject("target")),
                json.getString("title"),
                json.getStringOrNull("smallTitle")
            )
        }
    }
}