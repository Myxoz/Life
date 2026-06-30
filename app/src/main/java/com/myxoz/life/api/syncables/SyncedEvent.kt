package com.myxoz.life.api.syncables

import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.events.EmptyEvent
import com.myxoz.life.events.RawEvent
import com.myxoz.life.ui.feed.fullscreenevent.getId
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.getSafeLong
import org.json.JSONObject

class SyncedEvent(
    override val id: Long,
    val created: Long,
    val edited: Long?,
    val raw: RawEvent,
) : Syncable(raw.type.id, id) {
    override suspend fun specificsToJson(): JSONObject? =
        raw.toJson()
            .put("id", id.toString())
            .put("created", created)
            .put("edited", edited ?: JSONObject.NULL)

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        if(!isSynced())
            throw Error("Trying to save unsynced event to db. Aborting before harm.")
        else
            raw.saveEventToDB(db.eventDetailsDao, id, created, edited)
    }

    // override fun equals(other: Any?): Boolean = other is SyncedEvent && other.id == id

    fun copy(
        id: Long = this.id,
        created: Long = this.created,
        edited: Long? = this.edited,
        rawEvent: RawEvent = this.raw
    ) = SyncedEvent(id, created, edited, rawEvent)

    fun makeSynced() = if(!isSynced()) SyncedEvent(API.generateId(), created, edited, raw) else this

    fun copyWithTimes(start: Long = raw.start, end: Long = raw.end) =
        SyncedEvent(id, created, edited, raw.copyWithTimes(start, end))

    override fun getInvalidReason(): String? = raw.getInvalidReason()

    companion object : ServerSyncableCompanion<SyncedEvent> {
        fun from(preparedEventContent: RawEvent.PreparedEventContent): SyncedEvent? {
            return SyncedEvent(
                preparedEventContent.event.id,
                preparedEventContent.event.created,
                preparedEventContent.event.edited,
                RawEvent.from(preparedEventContent) ?: return null
            )
        }

        override fun fromJSON(json: JSONObject): SyncedEvent =
            SyncedEvent(
                json.getId(),
                json.getSafeLong("created"),
                json.getLongOrNull("edited"),
               RawEvent.fromJSON(json)
            )
        fun getSemanticNullValueEvent() = SyncedEvent(-1L, 0L, null, EmptyEvent(0L, 0L, false, usl = false))
    }
}