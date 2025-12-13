package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.HobbyEntiy
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.RenderTagAndTitleBar
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagEvent.Companion.getTagsFromJson
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.ui.theme.Colors
import org.json.JSONObject

@Composable
fun BoxScope.HobbyEventComposable(event: HobbyEvent, oneHourDp: Dp, startOfDay: Long, endOfDay: Long) {
    val blockHeight = event.getBlockHeight(startOfDay, endOfDay)
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxSize()
    ) {
        RenderTagAndTitleBar(event.eventTags, event.title, oneHourDp, blockHeight, Colors.Calendar.Hobby.Tag, Colors.Calendar.Hobby.TEXT)
        if(blockHeight>3) Text(
            event.details?:"",
            Modifier
                .padding(start = 10.dp, end = 3.dp)
            ,
            fontSize = (oneHourDp/3f).toSp(),
            color = Colors.Calendar.Hobby.SECONDARY,
            overflow = TextOverflow.Ellipsis
        )
    }
}

class HobbyEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    override val eventTags: List<EventTag>,
    override val title: String,
    override val details: String?
): ProposedEvent(start, end, EventType.Hobby, uss, usl), TagEvent, TitleEvent, DetailsEvent
{
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean {
        storeTags(db.tags, id)
        db.hobby.insertHobby(
            HobbyEntiy(
                id,
                title,
                details
            )
        )
        return true
    }
    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) {
        db.tags.removeById(id)
        db.hobby.removeById(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject.addTitle().addTags().addDetails()

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = HobbyEvent(start, end, uss, usl, eventTags, title, details)
    override fun getInvalidReason(): String? =
        if(title.isEmpty())
            "Gib einen Titel ein"
        else
            null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) = HobbyEvent(
            start, end, uss, usl, json.getTagsFromJson(), json.getString("title"), json.getStringOrNull("details")?.ifEmpty { null }
        )
        suspend fun from(db: StorageManager, event: EventEntity): HobbyEvent? {
            val hobbyEntity = db.hobby.getHobby(event.id) ?: return null
            return HobbyEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.tags.getTagsByEventId(event.id)
                    .mapNotNull { EventTag.getTagById(it) },
                hobbyEntity.title,
                hobbyEntity.details
            )
        }
    }
}
fun JSONObject.getStringOrNull(name: String): String? = if(isNull(name)) null else getString(name)
fun JSONObject.getLongOrNull(name: String): Long? = if(isNull(name)) null else getString(name).toLong()
