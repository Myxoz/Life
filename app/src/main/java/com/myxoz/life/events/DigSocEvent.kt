package com.myxoz.life.events

import android.content.SharedPreferences
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import com.myxoz.life.LocalScreens
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.android.autodetect.AutoDetectCall
import com.myxoz.life.dbwrapper.events.DigSocEntity
import com.myxoz.life.dbwrapper.events.DigSocMappingEntity
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.PeopleEvent.Companion.getPeopleFromJson
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderBasicEventContent
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.jsonObjArray
import org.json.JSONArray
import org.json.JSONObject

class DigSocEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    val digSocEntries: List<TimedTagLikeContainer<DigSocPlatform>>,
    override val title: String,
    override val people: List<Long>
): ProposedEvent(start, end, EventType.DigSoc, uss, usl), TitleEvent, PeopleEvent, AutoDetect.AutoDetectEvent
{
    override suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean {
        writeEventDetailsDao.insertDicSoc(
            DigSocEntity(
                id, title
            )
        )
        savePeopleMapping(writeEventDetailsDao, id, people)
        writeEventDetailsDao.insertAllDigSocMappings(
            digSocEntries.map {
                DigSocMappingEntity(id, it.type.id, it.durationMs)
            }
        )
        return true
    }

    @Composable
    override fun BoxScope.RenderContent(
        oneHourDp: Dp,
        startOfDay: Long,
        endOfDay: Long,
        isSmall: Boolean,
        blockHeight: Int
    ) {
        val profileViewModel = LocalScreens.current.profileInfoModel
        val people by profileViewModel.getPeople(people).collectAsState(listOf())
        val displayText = people.joinToString(" · ") { it.name }
        RenderBasicEventContent(
            title.ifBlank { if(blockHeight <= 3) displayText else title },
            displayText,
            displayText,
            isSmall,
            digSocEntries.map { it.type },
            OldColors.Calendar.DigSoc.TEXT,
            OldColors.Calendar.DigSoc.SECONDARY,
            OldColors.Calendar.DigSoc.Tag,
            oneHourDp,
            blockHeight
        )
    }

    override suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long) {
        db.removeDigSoc(id)
        db.deleteDigSocMapping(id)
        db.deletePeopleMapping(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject
        .addTitle()
        .addPeople()
        .put("mapping", JSONArray().apply { digSocEntries.forEach { put(it.timedTagLikeToJson()) } })

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = DigSocEvent(start, end, uss, usl, digSocEntries, title, people)
    override fun ignoreProposed(prefs: SharedPreferences) = ingoreAutoDetectable(this, AutoDetectCall.SPK, prefs)
    override fun getInvalidReason(): String? =
        if(digSocEntries.isEmpty())
            "Wähle mindestens eine Platform aus"
        else if(people.isEmpty())
            "Du bist nicht alleine"
        else
            null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) =
            DigSocEvent(
                start, end, uss, usl,
                json.getJSONArray("mapping").jsonObjArray.mapNotNull {
                    TimedTagLikeContainer(DigSocPlatform.getById(it.getInt("type"))?:return@mapNotNull null, it.getLong("duration_ms"))
                },
                json.getString("title"),
                getPeopleFromJson(json)
        )
        suspend fun from(db: ReadEventDetailsDao, event: EventEntity): DigSocEvent? {
            val socialEntity = db.getDigSoc(event.id) ?: return null
            return DigSocEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.getDigSocMappingByEventId(event.id)
                    .mapNotNull { TimedTagLikeContainer(DigSocPlatform.getById(it.app)?:return@mapNotNull null, it.durationMs) },
                socialEntity.title,
                db.getPeopleMappingsByEventId(event.id).map { it.personId }
            )
        }
    }
}