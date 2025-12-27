package com.myxoz.life.events

import android.content.Context
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import com.myxoz.life.api.API
import com.myxoz.life.calendar.feed.DefinedDurationEvent
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import com.myxoz.life.events.additionals.EventType
import org.json.JSONObject

abstract class ProposedEvent(start: Long, end: Long, val type: EventType, val uss: Boolean, val usl: Boolean): DefinedDurationEvent(start, end) {
    /**
     * Returns true if all entries were made successful, then entering will continue. This method is expected to revert all changes if false is ever returned
     */
    abstract suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean
    @Composable
    abstract fun BoxScope.RenderContent(oneHourDp: Dp, startOfDay: Long, endOfDay: Long, isSmall: Boolean, blockHeight: Int)
    protected abstract suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long)
    suspend fun eraseFromDB(db: StorageManager, id: Long) {
        db.events.removeById(id)
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
    suspend fun saveToEventTable(db: StorageManager, id: Long, created: Long, edited: Long?){
        db.events.insertEvent(
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
    /**
     * Saves to waitingSync, events and all event specific tables returns wether the action was successful (something changed)
     **/
    suspend fun insertAndSyncEvent(db: StorageManager, api: API, id: Long, created: Long, edited: Long?): Boolean{
        return if(saveEventSpecifics(db, id)) {
            saveToEventTable(db, id, created, edited)
            db.waitingSync.insertWaitingSync(
                WaitingSyncEntity(id, type.id, System.currentTimeMillis())
            )
            api.resyncLastDays()
            true
        } else {
            false
        }
    }

    open fun ignoreProposed(context: Context) {/* no-op */}
    abstract fun getInvalidReason(): String?
    fun overlaps(other: DefinedDurationEvent): Boolean = start < other.end && end > other.start

    companion object {
        suspend fun from(api: StorageManager, event: EventEntity): ProposedEvent? =
            when (event.type) {
                EventType.Sleep.id -> { SleepEvent.from(api, event) }
                EventType.Spont.id -> { SpontEvent.from(api, event) }
                EventType.Hobby.id -> { HobbyEvent.from(api, event) }
                EventType.Learn.id -> { LearnEvent.from(api, event) }
                EventType.Social.id -> { SocialEvent.from(api, event) }
                EventType.Travel.id -> { TravelEvent.from(api, event) }
                EventType.DigSoc.id -> { DigSocEvent.from(api, event) }
                else -> { null }
            }
        fun getProposedEventByJson(json: JSONObject): ProposedEvent? {
            val start = json.getLong("start")
            val end = json.getLong("end")
            val uss = json.getBoolean("uss")
            val usl = json.getBoolean("usl")
            val type = EventType.getById(json.getInt("type")) ?: return null
            return when(type) {
                EventType.Empty -> EmptyEvent(start, end, uss, usl)
                EventType.Sleep -> SleepEvent.fromJson(json, start, end, uss, usl)
                EventType.Spont -> SpontEvent.fromJson(json, start, end, uss, usl)
                EventType.Hobby -> HobbyEvent.fromJson(json, start, end, uss, usl)
                EventType.Learn -> LearnEvent.fromJson(json, start, end, uss, usl)
                EventType.Social -> SocialEvent.fromJson(json, start, end, uss, usl)
                EventType.Travel -> TravelEvent.fromJson(json, start, end, uss, usl)
                EventType.DigSoc -> DigSocEvent.fromJson(json, start, end, uss, usl)
            }
        }
    }
}