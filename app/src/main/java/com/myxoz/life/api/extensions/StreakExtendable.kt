package com.myxoz.life.api.extensions

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import com.myxoz.life.R
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.FullDaySyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.ui.feed.fullscreenevent.TimeBasedVisualTransformation
import com.myxoz.life.ui.feed.fullscreenevent.getId
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.UnixWeek
import com.myxoz.life.utils.asList
import com.myxoz.life.utils.asNotNullList
import com.myxoz.life.utils.formatMinutesToVisual
import com.myxoz.life.utils.formatMsToDuration
import com.myxoz.life.utils.getSafeLong
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.jsonArray
import com.myxoz.life.utils.nullIfEmpty
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class StreakExtendable(
    val streaks: List<StreakItem>
): ExtensionSyncable.Extendable {
    override fun specificsToString() = streaks.jsonArray { it.toJSON() }.toString()
    companion object: ExtensionSyncable.ExtendableCompanion<StreakExtendable>{
        override fun fromString(data: String): StreakExtendable = StreakExtendable(
            JSONArray(data).asList {
                StreakItem.fromJSON(getJSONObject(it))
            }
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
        abstract val bgColor: Color
        abstract val iconColor: Color
        abstract val icon: Int
        abstract fun specificsToJSON(): JSONObject
        fun toJSON(): JSONObject = specificsToJSON().put("streak_type", if(this is EventTarget) "event" else "day")
        fun continuesStreak(value: Long, threshold: Long, atLeast: Boolean): Boolean {
            return if(atLeast) value >= threshold else value < threshold
        }

        abstract fun format(current: Long): String
        abstract val visualTransformation: Lazy<VisualTransformation?>
        abstract fun stringToThreshold(enteredThreshold: String): Long
        abstract fun thresholdToString(current: Long): String
        abstract fun getInvalidReason(): String?

        data class EventTarget(
            val searchEventAmount: Boolean,
            val type: EventType,
            val tags: List<EventTag>,
            val people: List<Long>,
            val title: String?,
        ): StreakTarget() {
            override val visualTransformation = lazy { if(!searchEventAmount) TimeBasedVisualTransformation() else null }
            override fun stringToThreshold(enteredThreshold: String): Long =
                if(searchEventAmount)
                    enteredThreshold.nullIfEmpty()?.toLong() ?: 0L
                else
                    TimeBasedVisualTransformation.displayMinutesToMinutes(enteredThreshold) * 60 * 1000L

            override fun thresholdToString(current: Long): String =
                if(searchEventAmount) current.toString() else current.div(60*1000L).formatMinutesToVisual()

            override fun getInvalidReason() =
                when{
                    type == EventType.Empty -> "Wähle einen Eventyp aus"
                    else -> null
                }

            override val iconColor = type.colors.tagColors?.ICON ?: type.colors.textColor
            override val bgColor = type.colors.bg
            override val icon = tags.getOrNull(0)?.drawable ?: R.drawable.streak_icon
            override fun specificsToJSON(): JSONObject = JSONObject()
                .put("searchEventAmount", searchEventAmount)
                .put("type", type.id)
                .put("tags", tags.map { it.id }.jsonArray{ it })
                .put("people", people.jsonArray{ it.toString() })
                .putOpt("title", title)

            override fun format(current: Long): String = if(searchEventAmount) current.toString() else current.formatMsToDuration(true)

            fun currentStreakValue(events: List<SyncedEvent>, atLeast: Boolean, start: Long, end: Long): Long {
                fun isMatched(event: SyncedEvent) =
                    event.raw.type == type &&
                            (title == null || event.raw !is TitleEvent || event.raw.title == title) &&
                            (event.raw !is TagEvent || event.raw.eventTags.containsAll(tags)) &&
                            (event.raw !is PeopleEvent || event.raw.people.containsAll(people))
                val foundEvents = events
                    .filter {
                        isMatched(it) && (!searchEventAmount || it.raw.start >= start)
                    }
                if(searchEventAmount) return if(atLeast) foundEvents.size.toLong() else foundEvents.size.toLong()
                val eventLength = foundEvents.sumOf { it.raw.getDurationInRange(start, end) }
                return if(atLeast) eventLength else eventLength
            }
        }
        class DayTarget(
            val type: DayTargetType,
        ): StreakTarget() {
            override val bgColor = when(type)  {
                DayTargetType.Steps -> OldColors.STEPS
                DayTargetType.Screentime -> OldColors.SCREENTIME
            }
            override val icon = when(type)  {
                DayTargetType.Steps -> R.drawable.shoe
                DayTargetType.Screentime -> R.drawable.screentime
            }
            override fun format(current: Long): String = when(type) {
                DayTargetType.Steps -> current.toString().reversed().chunked(3).joinToString(".").reversed()
                DayTargetType.Screentime -> current.formatMsToDuration(true)
            }
            override val visualTransformation = lazy { if(type == DayTargetType.Screentime) TimeBasedVisualTransformation() else null }
            override fun stringToThreshold(enteredThreshold: String): Long =
                when(type){
                    DayTargetType.Steps -> {
                        enteredThreshold.nullIfEmpty()?.toLong() ?: 0L
                    }
                    DayTargetType.Screentime -> {
                        TimeBasedVisualTransformation.displayMinutesToMinutes(enteredThreshold) * 60 * 1000L
                    }
                }

            override fun thresholdToString(current: Long): String =
                when(type) {
                    DayTargetType.Steps -> current.toString()
                    DayTargetType.Screentime -> current.div(60*1000L).formatMinutesToVisual()
                }

            override fun getInvalidReason(): String? = null

            fun currentStreakValue(events: List<FullDaySyncable>): Long {
                return when(type) {
                    DayTargetType.Steps -> events.sumOf { it.steps }.toLong()
                    DayTargetType.Screentime -> events.sumOf { it.screenTimeMs.toLong() }
                }
            }
            override val iconColor = Color.White
            override fun specificsToJSON(): JSONObject = JSONObject()
                .put("type", type.jsonName)
        }
        companion object {
            fun fromJSON(json: JSONObject): StreakTarget {
                return if(json.getString("streak_type") == "event") {
                    EventTarget(
                        json.getBoolean("searchEventAmount"),
                        EventType.getById(json.getInt("type")) ?: error("Unknown event type in streak"),
                        json.getJSONArray("tags").asNotNullList { EventTag.getTagById(getInt(it)) },
                        json.getJSONArray("people").asList { getSafeLong(it) },
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
        fun getInvalidReason(): String? =
            when{
                title.isEmpty() -> "Titel darf nicht leer sein"
                else -> target.getInvalidReason()
            }
        fun toJSON() = JSONObject()
            .put("id", id.toString())
            .put("atLeast", atLeast)
            .put("threshold", threshold.toString())
            .put("timespan", timespan.jsonName)
            .put("target", target.toJSON())
            .put("title", title)
            .putOpt("smallTitle", smallTitle)
        fun formatThreshold(): String = target.format(threshold)
        companion object {
            fun fromJSON(json: JSONObject) = StreakItem(
                json.getId(),
                json.getBoolean("atLeast"),
                json.getSafeLong("threshold"),
                if(json.getString("timespan") == Timespan.Day.jsonName) Timespan.Day else Timespan.Week,
                StreakTarget.fromJSON(json.getJSONObject("target")),
                json.getString("title"),
                json.getStringOrNull("smallTitle")
            )
            @Composable
            fun StreakFlameActive(blurRadius: Dp){
                Image(
                    painterResource(R.drawable.streak_flame),
                    "",
                    Modifier
                        .blur(
                            blurRadius,
                            BlurredEdgeTreatment.Unbounded
                        )
                        .fillMaxSize()
                )
                Image(
                    painterResource(R.drawable.streak_flame),
                    "",
                    Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}