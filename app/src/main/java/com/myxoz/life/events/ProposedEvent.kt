package com.myxoz.life.events

import android.content.SharedPreferences
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.DefinedDurationEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.rippleClick
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

abstract class ProposedEvent(start: Long, end: Long, val type: EventType, val uss: Boolean, val usl: Boolean): DefinedDurationEvent(start, end) {
    /**
     * Returns true if all entries were made successful, then entering will continue. This method is expected to revert all changes if false is ever returned
     */
    abstract suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean
    @Composable
    abstract fun BoxScope.RenderContent(oneHourDp: Dp, startOfDay: Long, endOfDay: Long, isSmall: Boolean, blockHeight: Int)

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(oneHour: Dp, startOfDay: Long, endOfDay: Long, removeProposedEvent: ()->Unit, syncEvent: ()->Unit){
        Box(
            Modifier
                .padding(top = getTopPadding(oneHour, startOfDay))
                .height(getHeightDp(oneHour, startOfDay, endOfDay))
                .background(type.color.copy(.5f), RoundedCornerShape(10.dp))
                .fillMaxWidth()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(.5f)
            ) {
                RenderContent(oneHour, startOfDay, endOfDay, false,
                    getBlockHeight(startOfDay, endOfDay)
                )
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .fillMaxSize(),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(OldColors.ACCEPT)
                        .clip(RoundedCornerShape(10.dp))
                        .rippleClick {
                            syncEvent()
                            removeProposedEvent()
                        }
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(OldColors.DECLINE)
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(onLongClick = {
                            removeProposedEvent()
                        }) {}
                )
            }
        }
    }
    protected abstract suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long)
    suspend fun eraseFromDB(db: WriteEventDetailsDao, id: Long) {
        db.removeEventById(id)
        eraseEventSpecificsFromDB(db, id)
    }
    protected abstract fun addEventSpecifics(jsonObject: JSONObject): JSONObject
    fun getBackgroundBrush(alpha: Float = 1f) = if(!uss && !usl) SolidColor(type.color.copy(alpha)) else Brush.verticalGradient(
        listOf(
            if(uss) Color.Transparent else type.color.copy(alpha),
            type.color.copy(alpha),
            if(usl) Color.Transparent else type.color.copy(alpha)
        )
    )
    fun toJson(): JSONObject =
        addEventSpecifics(
            JSONObject()
                .put("start", start)
                .put("end", end)
                .put("type", type.id)
                .put("uss", uss)
                .put("usl", usl)
        )
    abstract fun copyWithTimes(start: Long, end: Long, uss: Boolean=this.uss, usl: Boolean=this.usl): ProposedEvent
    suspend fun saveToEventTable(writeEventDetailsDao: WriteEventDetailsDao, id: Long, created: Long, edited: Long?){
        writeEventDetailsDao.insertEvent(
            EventEntity(
                id,
                type.id,
                start,
                end,
                created,
                edited,
                uss,
                usl
            )
        )
    }
    /** Saves to waitingSync, events and all event specific tables returns wether the action was successful (something changed) **/
    suspend fun saveEventToDB(writeEventDetailsDao: WriteEventDetailsDao, id: Long, created: Long, edited: Long?): Boolean{
        return if(saveEventSpecifics(writeEventDetailsDao, id)) {
            saveToEventTable(writeEventDetailsDao, id, created, edited)
            true
        } else {
            false
        }
    }

    open fun ignoreProposed(prefs: SharedPreferences) {/* no-op */}
    abstract fun getInvalidReason(): String?
    fun overlaps(other: DefinedDurationEvent): Boolean = start < other.end && end > other.start

    fun getAllStrechedDays(zone: ZoneId): List<LocalDate> {
        val startDate = Instant.ofEpochSecond(start/1000).atZone(zone).toLocalDate()
        val endDate = Instant.ofEpochSecond(end/1000).atZone(zone).toLocalDate()

        val dates = mutableListOf<LocalDate>()
        var current = startDate

        while (!current.isAfter(endDate)) {
            dates += current
            current = current.plusDays(1)
        }

        return dates
    }

    companion object {
        suspend fun from(db: ReadEventDetailsDao, event: EventEntity): ProposedEvent? =
            when (event.type) {
                EventType.Sleep.id -> SleepEvent.from(db, event)
                EventType.Spont.id -> SpontEvent.from(db, event)
                EventType.Hobby.id -> HobbyEvent.from(db, event)
                EventType.Learn.id -> LearnEvent.from(db, event)
                EventType.Social.id -> SocialEvent.from(db, event)
                EventType.Travel.id -> TravelEvent.from(db, event)
                EventType.DigSoc.id -> DigSocEvent.from(db, event)
                EventType.Work.id -> WorkEvent.from(db, event)
                else -> null
            }
        fun getProposedEventByJson(json: JSONObject): ProposedEvent {
            val start = json.getLong("start")
            val end = json.getLong("end")
            val uss = json.getBoolean("uss")
            val usl = json.getBoolean("usl")
            val type = EventType.getById(json.getInt("type")).let {
                if (it == null)
                    throw Error("Tried to construct proposed event with unknown type ${json.getInt("type")}")
                it
            }

            return when(type) {
                EventType.Empty -> EmptyEvent(start, end, uss, usl)
                EventType.Sleep -> SleepEvent.fromJson(json, start, end, uss, usl)
                EventType.Spont -> SpontEvent.fromJson(json, start, end, uss, usl)
                EventType.Hobby -> HobbyEvent.fromJson(json, start, end, uss, usl)
                EventType.Learn -> LearnEvent.fromJson(json, start, end, uss, usl)
                EventType.Social -> SocialEvent.fromJson(json, start, end, uss, usl)
                EventType.Travel -> TravelEvent.fromJson(json, start, end, uss, usl)
                EventType.DigSoc -> DigSocEvent.fromJson(json, start, end, uss, usl)
                EventType.Work -> WorkEvent.fromJson(json, start, end, uss, usl)
            }
        }
    }
}