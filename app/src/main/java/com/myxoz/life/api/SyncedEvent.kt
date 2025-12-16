package com.myxoz.life.api

import android.util.Log
import com.myxoz.life.calendar.getEventId
import com.myxoz.life.dbwrapper.EventEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.utils.getStringOrNull
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

    override suspend fun specificsToJson(db: StorageManager): JSONObject =
        proposed.toJson()
            .put("id", id.toString())
            .put("created", created)
            .put("edited", edited ?: JSONObject.NULL)

    override suspend fun saveToDB(db: StorageManager) {
        Log.w("API", "Noop happened in SyncedEvent.saveToDB, DO NOT USE THIS")
    }

    fun copy(
        id: Long = this.id,
        created: Long = this.created,
        edited: Long? = this.edited,
        proposedEvent: ProposedEvent = this.proposed
    ) = SyncedEvent(id, created, edited, proposedEvent)

    fun copyWithTimes(start: Long = proposed.start, end: Long = proposed.end) =
        SyncedEvent(id, created, edited, proposed.copyWithTimes(start, end))

    companion object : ServerSyncableCompanion {
        suspend fun from(db: StorageManager, event: EventEntity): SyncedEvent? {
            return SyncedEvent(
                event.id,
                event.created,
                event.edited,
                ProposedEvent.from(db, event) ?: return null
            )
        }

        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            val type = EventType.getById(it.getInt("type"))
            val id = it.getEventId()
            if (type == null) {
                Log.w("API", "Server responded with unknown event type ${it.getInt("type")}")
                return
            }
            val proposedEvent = ProposedEvent.getProposedEventByJson(it) ?: return
            proposedEvent.eraseFromDB(db, id)
            if (proposedEvent.saveEventSpecifics(db, id)) {
                proposedEvent.saveToEventTable(
                    db,
                    id,
                    it.getLong("created"),
                    it.getStringOrNull("edited")?.toLongOrNull()
                )
            }
        }
    }
}