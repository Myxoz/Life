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
import com.myxoz.life.dbwrapper.events.DigSocEntity
import com.myxoz.life.dbwrapper.events.DigSocMappingEntity
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.HobbyEntiy
import com.myxoz.life.dbwrapper.events.LearnEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.SocialEntity
import com.myxoz.life.dbwrapper.events.SpontEntity
import com.myxoz.life.dbwrapper.events.TimewasteEntity
import com.myxoz.life.dbwrapper.events.TimewastePlatformEntity
import com.myxoz.life.dbwrapper.events.TravelEntity
import com.myxoz.life.dbwrapper.events.VehicleEntity
import com.myxoz.life.dbwrapper.events.WorkEntity
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.DefinedDurationEvent
import com.myxoz.life.events.additionals.EventTag
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
                .background(type.colors.bg.copy(.5f), RoundedCornerShape(10.dp))
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
    fun getBackgroundBrush(alpha: Float = 1f) = if(!uss && !usl) SolidColor(type.colors.bg.copy(alpha)) else Brush.verticalGradient(
        listOf(
            if(uss) Color.Transparent else type.colors.bg.copy(alpha),
            type.colors.bg.copy(alpha),
            if(usl) Color.Transparent else type.colors.bg.copy(alpha)
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

    data class PreparedEventContent(
        val event: EventEntity,
        val tags: List<EventTag>?,
        val spont: SpontEntity?,
        val hobby: HobbyEntiy?,
        val learn: LearnEntity?,
        val social: SocialEntity?,
        val travel: TravelEntity?,
        val digSoc: DigSocEntity?,
        val work: WorkEntity?,
        val timewaste: TimewasteEntity?,
        val people: List<Long>?,
        val vehicle: List<VehicleEntity>?,
        val digSocMapping: List<DigSocMappingEntity>?,
        val twPlatform: List<TimewastePlatformEntity>?,
    ) {
        companion object {
            suspend fun prepareContentFor(events: List<EventEntity>, db: ReadEventDetailsDao): List<PreparedEventContent> {
                val ids = events.map { it.id }
                val allTypes = events.map { it.type }.toSet()
                val tags = if(allTypes.any { EventType.getById(it)?.isTagEvent() ?: false}) db.getAllTagsFor(ids).groupBy { it.id }.mapValues { event ->
                    event.value.mapNotNull { EventTag.getTagById(it.tag) }
                } else emptyMap()
                val spont = if(EventType.Spont.id in allTypes) db.getAllSpontFor(ids).associateBy { it.id } else emptyMap()
                val hobby = if(EventType.Hobby.id in allTypes) db.getAllHobbyFor(ids).associateBy { it.id } else emptyMap()
                val learn = if(EventType.Learn.id in allTypes) db.getAllLearnFor(ids).associateBy { it.id } else emptyMap()
                val social = if(EventType.Social.id in allTypes) db.getAllSocialFor(ids).associateBy { it.id } else emptyMap()
                val travel = if(EventType.Travel.id in allTypes) db.getAllTravelFor(ids).associateBy { it.id } else emptyMap()
                val digSoc = if(EventType.DigSoc.id in allTypes) db.getAllDigSocFor(ids).associateBy { it.id } else emptyMap()
                val work = if(EventType.Work.id in allTypes) db.getAllWorkFor(ids).associateBy { it.id } else emptyMap()
                val timewaste = if(EventType.Timewaste.id in allTypes) db.getAllTimewasteFor(ids).associateBy { it.id } else emptyMap()
                val people = if(EventType.DigSoc.id in allTypes || EventType.Social.id in allTypes) db.getAllPeopleFor(ids).groupBy { it.eventId }.mapValues { event ->
                    event.value.map { it.personId }
                } else emptyMap()
                val vehicle = if(EventType.Travel.id in allTypes) db.getAllVehiclesFor(ids).groupBy { it.id } else emptyMap()
                val digSocMapping = if(EventType.DigSoc.id in allTypes) db.getAllDigSocMappingFor(ids).groupBy { it.eventId } else emptyMap()
                val twPlatform = if(EventType.Timewaste.id in allTypes) db.getAllTimewastePlatformsFor(ids).groupBy { it.eventId } else emptyMap()
                return events.map {
                    PreparedEventContent(
                        it,
                        tags[it.id],
                        spont[it.id],
                        hobby[it.id],
                        learn[it.id],
                        social[it.id],
                        travel[it.id],
                        digSoc[it.id],
                        work[it.id],
                        timewaste[it.id],
                        people[it.id],
                        vehicle[it.id],
                        digSocMapping[it.id],
                        twPlatform[it.id],
                    )
                }
            }
            suspend fun prepareContentFor(entity: EventEntity, db: ReadEventDetailsDao): PreparedEventContent? {
                val eventType = EventType.getById(entity.type) ?: return null
                val event = entity.id
                return PreparedEventContent(
                    entity,
                    if(eventType.isTagEvent()) db.getTagsByEventId(event).mapNotNull { EventTag.getTagById(it) } else null,
                    if(eventType == EventType.Spont) db.getSpont(event) else null,
                    if(eventType == EventType.Hobby) db.getHobby(event) else null,
                    if(eventType == EventType.Learn) db.getLearn(event) else null,
                    if(eventType == EventType.Social) db.getSocial(event) else null,
                    if(eventType == EventType.Travel) db.getTavel(event) else null,
                    if(eventType == EventType.DigSoc) db.getDigSoc(event) else null,
                    if(eventType == EventType.Work) db.getWork(event) else null,
                    if(eventType == EventType.Timewaste) db.getTimewaste(event) else null,
                    if(eventType == EventType.Social || eventType == EventType.DigSoc) db.getPeopleMappingsByEventId(event).map { it.personId } else null,
                    if(eventType == EventType.Travel) db.getVehicles(event) else null,
                    if(eventType == EventType.DigSoc) db.getDigSocMappingByEventId(event) else null,
                    if(eventType == EventType.Timewaste) db.getTimewastePlatformsById(event) else null,
                )
            }
            suspend fun prepareContentFor(event: Long, db: ReadEventDetailsDao): PreparedEventContent? {
                return prepareContentFor(
                    db.getEvent(event) ?: return null,
                    db
                )
            }
        }
    }
    companion object {
        fun from(pec: PreparedEventContent): ProposedEvent? {
            return when (pec.event.type) {
                EventType.Sleep.id -> SleepEvent.from(pec.event)
                EventType.Spont.id -> SpontEvent.from(pec.event, pec.spont ?: return null, pec.tags)
                EventType.Hobby.id -> HobbyEvent.from(pec.event, pec.hobby ?: return null, pec.tags)
                EventType.Learn.id -> LearnEvent.from(pec.event, pec.learn ?: return null, pec.tags)
                EventType.Social.id -> SocialEvent.from(pec.event, pec.social ?: return null, pec.people, pec.tags)
                EventType.Travel.id -> TravelEvent.from(pec.event, pec.travel ?: return null, pec.vehicle)
                EventType.DigSoc.id -> DigSocEvent.from(pec.event, pec.digSoc ?: return null, pec.digSocMapping, pec.people)
                EventType.Work.id -> WorkEvent.from(pec.event, pec.work ?: return null, pec.tags)
                EventType.Timewaste.id -> TimewasteEvent.from(pec.event, pec.timewaste ?: return null, pec.twPlatform)
                else -> error("Unknown event loaded from DB $pec")
            }
        }
        fun fromJSON(json: JSONObject): ProposedEvent {
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
                EventType.Timewaste -> TimewasteEvent.fromJson(json, start, end, uss, usl)
            }
        }
    }
}