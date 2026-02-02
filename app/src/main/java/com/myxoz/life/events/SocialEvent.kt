package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalScreens
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.SocialEntity
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.PeopleEvent.Companion.getPeopleFromJson
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagEvent.Companion.getTagsFromJson
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderTagAndTitleBar
import com.myxoz.life.ui.theme.OldColors
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
    override suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean {
        storeTags(writeEventDetailsDao, id)
        writeEventDetailsDao.insertSocial(
            SocialEntity(
                id,
                title,
                more
            )
        )
        savePeopleMapping(writeEventDetailsDao, id, people)
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
        if(isSmall){
            Text(
                displayText,
                Modifier
                    .padding(all = 3.dp),
                fontSize = (oneHourDp / 4f).toSp(),
                color = OldColors.Calendar.Social.SECONDARY,
                overflow = TextOverflow.Ellipsis
            )
        } else Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
        ) {
            RenderTagAndTitleBar(eventTags, title, oneHourDp, blockHeight, OldColors.Calendar.Social.Tag, OldColors.Calendar.Social.TEXT)
            if(blockHeight>3) {
                Text(
                    displayText,
                    Modifier
                        .padding(start = 10.dp),
                    fontSize = (oneHourDp / 3f).toSp(),
                    color = OldColors.Calendar.Social.SECONDARY,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if(more) "+ Weitere" else "",
                    Modifier
                        .padding(start = 10.dp),
                    fontSize = (oneHourDp / 3f).toSp(),
                    color = OldColors.Calendar.Social.SECONDARY,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    override suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long) {
        db.removeTags(id)
        db.removeSocial(id)
        db.deletePeopleMapping(id)
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
        suspend fun from(db: ReadEventDetailsDao, event: EventEntity): SocialEvent? {
            val socialEntity = db.getSocial(event.id) ?: return null
            return SocialEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.getTagsByEventId(event.id).mapNotNull { EventTag.getTagById(it) },
                socialEntity.title,
                db.getPeopleMappingsByEventId(event.id).map { it.personId },
                socialEntity.more
            )
        }
    }
}