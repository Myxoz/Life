package com.myxoz.life.api.syncables

import androidx.compose.runtime.Composable
import com.myxoz.life.R
import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.todos.TodoEntity
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.screens.feed.instantevents.InstantEvent
import com.myxoz.life.utils.getServerBoolean
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.nullIfEmpty
import org.json.JSONObject

data class TodoSyncable(
    override val id: Long,
    val short: String,
    val details: String?,
    val done: Boolean,
    override val timestamp: Long,
): Syncable(SpecialSyncablesIds.TODOS, id), Syncable.FeedInstantEventSyncable {
    override suspend fun specificsToJson(): JSONObject? = JSONObject()
        .put("short", short)
        .put("details", details)
        .put("done", done)
        .put("timestamp", timestamp)

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.todosDao.insertTodo(
            TodoEntity(id, short, details, done, timestamp)
        )
    }

    override fun copyWithTs(timestamp: Long) = copy(timestamp = timestamp)
    override fun asInstantEvent(): InstantEvent = InstantEvent(
        R.drawable.todos_icon,
        short,
        timestamp,
        { it is TodoSyncable && it.id == id },
    ) { screens ->
        screens.fullScreenTodo(this)
    }

    override suspend fun delete(repos: AppRepositories) {
        repos.todoRepo.deleteTodoFromServerDatabaseAndCache(this)
    }

    override suspend fun saveWithCache(repos: AppRepositories) {
        repos.todoRepo.savePotentiallyUnsynced(this)
    }

    override fun getFeedInvalidReason(): String? = when {
        short.isBlank() -> "Gib eine Kurzbeschreibung ein"
        else -> null
    }

    override fun getInvalidReason(): String? = getFeedInvalidReason()

    fun ensureSynced() = if(!isSynced()) copy(id = API.generateId()) else this

    @Composable
    override fun ModifyEventInputs(setSyncableTo: (FeedInstantEventSyncable) -> Unit) {
        InputField(short.ifEmpty { null }, "Kategorie/Kurzfassung") {
            setSyncableTo(copy(short = it))
        }
        InputField(details?.ifEmpty { null }, "Beschreibung") {
            setSyncableTo(copy(details = it.nullIfEmpty()))
        }
    }

    companion object: ServerSyncableCompanion<TodoSyncable>{
        override fun fromJSON(json: JSONObject) = TodoSyncable(
            json.getId(),
            json.getString("short"),
            json.getStringOrNull("details"),
            json.getServerBoolean("done"),
            json.getLong("timestamp")
        )
        fun fromEntity(entity: TodoEntity) = TodoSyncable(
            entity.id,
            entity.short,
            entity.details,
            entity.done,
            entity.timestamp
        )
    }
}