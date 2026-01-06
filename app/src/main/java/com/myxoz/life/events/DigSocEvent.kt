package com.myxoz.life.events

import android.content.Context
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.myxoz.life.LocalStorage
import com.myxoz.life.android.autodetect.AutoDetect
import com.myxoz.life.android.autodetect.AutoDetectCall
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.dbwrapper.DigSocEntity
import com.myxoz.life.dbwrapper.DigSocMappingEntity
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.DigSocPlatform
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.PeopleEvent.Companion.getPeopleFromJson
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderBasicEventContent
import com.myxoz.life.ui.theme.Colors
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
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean {
        db.digsoc.insertEvent(
            DigSocEntity(
                id, title
            )
        )
        savePeopleMapping(db, id, people)
        db.digsocMapping.insertAll(
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
        val db = LocalStorage.current
        var displayText by remember { mutableStateOf("") }
        PeopleEvent.GetFullNames(db, people) { persons ->
            displayText = persons.joinToString(" · ") { it.name }
        }
        RenderBasicEventContent(
            title.ifBlank { if(blockHeight <= 3) displayText else title },
            displayText,
            displayText,
            isSmall,
            digSocEntries.map { it.type },
            Colors.Calendar.DigSoc.TEXT,
            Colors.Calendar.DigSoc.SECONDARY,
            Colors.Calendar.DigSoc.Tag,
            oneHourDp,
            blockHeight
        )
    }

    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) {
        db.digsoc.removeById(id)
        db.peopleMapping.deleteMappingByEventId(id)
        db.digsocMapping.deleteMappingByEventId(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject
        .addTitle()
        .addPeople()
        .put("mapping", JSONArray().apply { digSocEntries.forEach { put(it.timedTagLikeToJson()) } })

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = DigSocEvent(start, end, uss, usl, digSocEntries, title, people)
    override fun ignoreProposed(context: Context) = ingoreAutoDetectable(this, AutoDetectCall.SPK, context)
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
        suspend fun from(db: StorageManager, event: EventEntity): DigSocEvent? {
            val socialEntity = db.digsoc.getEventById(event.id) ?: return null
            return DigSocEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.digsocMapping.getMappingByEventId(event.id)
                    .mapNotNull { TimedTagLikeContainer(DigSocPlatform.getById(it.app)?:return@mapNotNull null, it.durationMs) },
                socialEntity.title,
                db.peopleMapping.getMappingsByEventId(event.id).map { it.personId }
            )
        }
    }
}