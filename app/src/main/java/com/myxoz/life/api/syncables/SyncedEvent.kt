package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.events.EventEntity
import com.myxoz.life.dbwrapper.events.ReadEventDetailsDao
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.api.API
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.getLongOrNull
import org.json.JSONObject

class SyncedEvent(
    id: Long,
    val created: Long,
    val edited: Long?,
    val proposed: ProposedEvent,
) : Syncable(proposed.type.id, id) {
    fun onlyToJson(): JSONObject = JSONObject()
        .put("id", id.toString())
        .put("created", created)
        .put("edited", edited ?: JSONObject.NULL)

    override suspend fun specificsToJson(): JSONObject? =
        proposed.toJson()
            .put("id", id.toString())
            .put("created", created)
            .put("edited", edited ?: JSONObject.NULL)

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        if(!isSynced())
            throw Error("Trying to save unsynced event to db. Aborting before harm.")
        else
            proposed.saveEventToDB(db.eventDetailsDao, id, created, edited)
    }

    // override fun equals(other: Any?): Boolean = other is SyncedEvent && other.id == id

    fun copy(
        id: Long = this.id,
        created: Long = this.created,
        edited: Long? = this.edited,
        proposedEvent: ProposedEvent = this.proposed
    ) = SyncedEvent(id, created, edited, proposedEvent)

    fun makeSynced() = SyncedEvent(if(isSynced()) id else API.generateId(), created, edited, proposed)

    fun isSynced() = id > 0

    fun copyWithTimes(start: Long = proposed.start, end: Long = proposed.end) =
        SyncedEvent(id, created, edited, proposed.copyWithTimes(start, end))

    companion object : ServerSyncableCompanion<SyncedEvent> {
        suspend fun from(db: ReadEventDetailsDao, event: EventEntity): SyncedEvent? {
            return SyncedEvent(
                event.id,
                event.created,
                event.edited,
                ProposedEvent.from(db, event) ?: return null
            )
        }

        override fun fromJSON(json: JSONObject): SyncedEvent =
            SyncedEvent(
                json.getId(),
                json.getString("created").toLong(),
                json.getLongOrNull("created"),
               ProposedEvent.getProposedEventByJson(json)
            )
    }
}