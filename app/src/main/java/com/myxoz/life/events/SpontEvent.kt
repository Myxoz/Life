package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.SpontEntity
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.TagEvent.Companion.getTagsFromJson
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderTagAndTitleBar
import com.myxoz.life.ui.theme.OldColors
import org.json.JSONObject

class SpontEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    override val eventTags: List<EventTag>,
    override val title: String
): ProposedEvent(start, end, EventType.Spont, uss, usl), TagEvent, TitleEvent {
    override suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean {
        storeTags(writeEventDetailsDao, id)
        writeEventDetailsDao.insertSpont(
            SpontEntity(
                id,
                title
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
        RenderTagAndTitleBar(eventTags, title, oneHourDp, blockHeight, OldColors.Calendar.Spont.Tag, OldColors.Calendar.Spont.TEXT)
    }

    override suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long) {
        db.removeTags(id)
        db.removeSpont(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject.addTitle().addTags()

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = SpontEvent(start, end, uss, usl, eventTags, title)
    override fun getInvalidReason(): String? =
        if(title.isEmpty())
            "Gib einen Titel ein"
        else
            null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) = SpontEvent(
            start, end, uss, usl, json.getTagsFromJson(),
            json.getString("title")
        )
        suspend fun from(db: ReadEventDetailsDao, event: EventEntity) = SpontEvent(
            event.start,
            event.end,
            event.uss,
            event.usl,
            db.getTagsByEventId(event.id)
                .mapNotNull { EventTag.getTagById(it) },
            db.getSpont(event.id)?.title ?: ""
        )
    }
}