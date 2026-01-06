package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.SocialEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.PeopleEvent.Companion.getPeopleFromJson
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagEvent.Companion.getTagsFromJson
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderTagAndTitleBar
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.utils.toSp
import org.json.JSONObject

class SocialEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    override val eventTags: List<EventTag>,
    override val title: String,
    override val people: List<Long>,
    val more: Boolean
): ProposedEvent(start, end, EventType.Social, uss, usl), TagEvent, TitleEvent, PeopleEvent
{
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean {
        storeTags(db.tags, id)
        db.social.insertEvent(
            SocialEntity(
                id,
                title,
                more
            )
        )
        savePeopleMapping(db, id, people)
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
            RenderTagAndTitleBar(eventTags, title, oneHourDp, blockHeight, Colors.Calendar.Social.Tag, Colors.Calendar.Social.TEXT)
            if(blockHeight>3) {
                Text(
                    displayText,
                    Modifier
                        .padding(start = 10.dp),
                    fontSize = (oneHourDp / 3f).toSp(),
                    color = Colors.Calendar.Social.SECONDARY,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if(more) "+ Weitere" else "",
                    Modifier
                        .padding(start = 10.dp),
                    fontSize = (oneHourDp / 3f).toSp(),
                    color = Colors.Calendar.Social.SECONDARY,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) {
        db.tags.removeById(id)
        db.social.removeById(id)
        db.peopleMapping.deleteMappingByEventId(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject
        .addTitle()
        .addTags()
        .addPeople()
        .put("more", more)

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = SocialEvent(start, end, uss, usl, eventTags, title, people, more)
    override fun getInvalidReason(): String? =
        if(title.isEmpty())
            "Gib einen Titel ein"
        else if(people.isEmpty() && !more)
            "Du bist nicht alleine"
        else
            null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) =
            SocialEvent(start, end, uss, usl, json.getTagsFromJson(), json.getString("title"), getPeopleFromJson(json), json.getBoolean("more")
        )
        suspend fun from(db: StorageManager, event: EventEntity): SocialEvent? {
            val socialEntity = db.social.getEvent(event.id) ?: return null
            return SocialEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.tags.getTagsByEventId(event.id)
                    .mapNotNull { EventTag.getTagById(it) },
                socialEntity.title,
                db.peopleMapping.getMappingsByEventId(event.id)
                    .map { it.personId },
                socialEntity.more
            )
        }
    }
}