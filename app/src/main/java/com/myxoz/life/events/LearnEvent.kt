package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.LearnEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.additionals.DetailsEvent
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagEvent.Companion.getTagsFromJson
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderBasicEventContent
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.utils.getStringOrNull
import org.json.JSONObject

class LearnEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    override val eventTags: List<EventTag>,
    override val title: String,
    override val details: String?
): ProposedEvent(start, end, EventType.Learn, uss, usl), TagEvent, TitleEvent, DetailsEvent
{
    override suspend fun saveEventSpecifics(db: StorageManager, id: Long): Boolean {
        storeTags(db.tags, id)
        db.learn.insertEvent(
            LearnEntity(
                id,
                title,
                details
            )
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
        val blockHeight = getBlockHeight(startOfDay, endOfDay)
        RenderBasicEventContent(
            title,
            details,
            title,
            isSmall,
            eventTags,
            OldColors.Calendar.Learn.TEXT,
            OldColors.Calendar.Learn.SECONDARY,
            OldColors.Calendar.Learn.Tag,
            oneHourDp,
            blockHeight
        )
    }

    override suspend fun eraseEventSpecificsFromDB(db: StorageManager, id: Long) {
        db.tags.removeById(id)
        db.learn.removeById(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject.addTitle().addTags().addDetails()

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = LearnEvent(start, end, uss, usl, eventTags, title, details)
    override fun getInvalidReason(): String? =
        if(title.isEmpty())
            "Gib einen Titel ein"
        else
            null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) = LearnEvent(
            start, end, uss, usl, json.getTagsFromJson(), json.getString("title"), json.getStringOrNull("details")
        )
        suspend fun from(db: StorageManager, event: EventEntity): LearnEvent? {
            val learnEntity = db.learn.getEvent(event.id) ?: return null
            return LearnEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.tags.getTagsByEventId(event.id)
                    .mapNotNull { EventTag.getTagById(it) },
                learnEntity.title,
                learnEntity.details
            )
        }
    }
}