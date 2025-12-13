package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.dbwrapper.DigSocEntity
import com.myxoz.life.dbwrapper.DigSocMappingEntity
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.PeopleEvent.Companion.getPeopleFromJson
import com.myxoz.life.events.additionals.RenderTagAndTitleBar
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.ui.theme.Colors
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun BoxScope.DigSocEventComposable(event: DigSocEvent, oneHourDp: Dp, startOfDay: Long, endOfDay: Long, isSmall: Boolean, blockHeight: Int) {
    val db = LocalStorage.current
    var displayText by remember { mutableStateOf("") }
    PeopleEvent.GetFullNames(db, event.people) { persons ->
        displayText = persons.joinToString(" · ") { it.name }
    }
    if(isSmall){
        Text(
            displayText,
            Modifier
                .padding(all = 3.dp),
            fontSize = (oneHourDp / 4f).toSp(),
            color = Colors.Calendar.Social.SECONDARY,
            overflow = TextOverflow.Ellipsis
        )
    } else Column(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxSize()
    ) {
        val titleText = event.title.ifBlank { if(blockHeight <= 3) displayText else event.title }
        RenderTagAndTitleBar(event.digSocEntries.map { it.type }, titleText, oneHourDp, blockHeight, Colors.Calendar.DigSoc.Tag, Colors.Calendar.DigSoc.TEXT)
        if(blockHeight > 3) {
            Text(
                displayText,
                Modifier
                    .padding(start = 10.dp),
                fontSize = (oneHourDp / 3f).toSp(),
                color = Colors.Calendar.DigSoc.SECONDARY,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

class DigSocEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    val digSocEntries: List<TimedTagLikeContainer<DigSocPlatform>>,
    override val title: String,
    override val people: List<Long>
): ProposedEvent(start, end, EventType.DigSoc, uss, usl), TitleEvent, PeopleEvent
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
enum class DigSocPlatform(override val id: Int, override val drawable: Int): TagLike {
    Call(1, R.drawable.phone),
    Instagram(2, R.drawable.insta),
    WhatsApp(3, R.drawable.whatsapp),
    Snapchat(4, R.drawable.snap),
    Message(5, R.drawable.message),
    ;
    companion object {
        fun getById(id: Int): DigSocPlatform? = DigSocPlatform.entries.firstOrNull { it.id == id }.apply { if(this==null) println("Couldnt find app with id $id in DigSocEntry.getById") }
    }
}