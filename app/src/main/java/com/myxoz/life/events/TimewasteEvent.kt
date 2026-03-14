package com.myxoz.life.events

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.dbwrapper.events.TimewasteEntity
import com.myxoz.life.dbwrapper.events.TimewastePlatformEntity
import com.myxoz.life.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.TimedTagLikeContainer
import com.myxoz.life.events.additionals.TimewastePlatform
import com.myxoz.life.events.additionals.TitleEvent
import com.myxoz.life.screens.feed.main.RenderTagAndTitleBar
import com.myxoz.life.utils.jsonObjArray
import org.json.JSONArray
import org.json.JSONObject

class TimewasteEvent(
    start: Long,
    end: Long,
    uss: Boolean,
    usl: Boolean,
    val timewastePlatforms: List<TimedTagLikeContainer<TimewastePlatform>>,
    override val title: String,
): ProposedEvent(start, end, EventType.Timewaste, uss, usl), TitleEvent
{
    override suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean {
        writeEventDetailsDao.insertTimewaste(
            TimewasteEntity(id, title)
        )
        writeEventDetailsDao.insertTimewastePlatforms(
            timewastePlatforms.map {
                TimewastePlatformEntity(id, it.type.id, it.durationMs)
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
        RenderTagAndTitleBar(
            timewastePlatforms.map { it.type },
            title,
            oneHourDp,
            blockHeight,
            type.colors,
        )
    }

    override suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long) {
        db.removeDigSoc(id)
        db.deleteDigSocMapping(id)
        db.deletePeopleMapping(id)
    }
    override fun addEventSpecifics(jsonObject: JSONObject): JSONObject = jsonObject
        .addTitle()
        .put("mapping", JSONArray().apply { timewastePlatforms.forEach { put(it.timedTagLikeToJson()) } })

    override fun copyWithTimes(start: Long, end: Long, uss: Boolean, usl: Boolean) = TimewasteEvent(start, end, uss, usl, timewastePlatforms, title)
    override fun getInvalidReason(): String? =
        if(timewastePlatforms.isEmpty())
            "Wähle mindestens eine Platform aus"
        else
            null

    companion object {
        fun fromJson(json: JSONObject, start: Long, end: Long, uss: Boolean, usl: Boolean) =
            TimewasteEvent(
                start, end, uss, usl,
                json.getJSONArray("mapping").jsonObjArray.mapNotNull {
                    TimedTagLikeContainer(TimewastePlatform.getById(it.getInt("type"))?:return@mapNotNull null, it.getLong("duration_ms"))
                },
                json.getString("title")
        )
        suspend fun from(db: ReadEventDetailsDao, event: EventEntity): TimewasteEvent? {
            val timewasteEntity = db.getTimewaste(event.id) ?: return null
            return TimewasteEvent(
                event.start,
                event.end,
                event.uss,
                event.usl,
                db.getTimewastePlatformsById(event.id)
                    .mapNotNull { TimedTagLikeContainer(TimewastePlatform.getById(it.timewastePlatform)?:return@mapNotNull null, it.durationMs) },
                timewasteEntity.title?:""
            )
        }
    }
}