package com.myxoz.life.api.syncables

import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.extensions.FavoriteContactsExtendable
import com.myxoz.life.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.storage.dbwrapper.extension.ExtensionEntity
import com.myxoz.life.ui.feed.fullscreenevent.getId
import org.json.JSONObject

class ExtensionSyncable(
    val type: ExtensionSyncableType<Extendable>,
    val content: Extendable
): Syncable(SpecialSyncablesIds.EXTENSION, type.id) {
    override suspend fun specificsToJson(): JSONObject = JSONObject()
        .put("data", content.specificsToString())
        // Yes this doesn't put JSON but a JSON string, as it's supposed to

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.extensionDao.insertExtension(
            ExtensionEntity(
                type.id,
                content.specificsToString()
            )
        )
    }

    sealed class ExtensionSyncableType<out T: Extendable>(
        val id: Long,
        val parser: ExtendableCompanion<T>
    ) {
        object Streak: ExtensionSyncableType<StreakExtendable>(
            1,
            StreakExtendable
        )
        object FavoriteContacts: ExtensionSyncableType<FavoriteContactsExtendable>(
            2,
            FavoriteContactsExtendable
        )
        object RepeatingEvents: ExtensionSyncableType<RepeatingEventsExtendable>(
            3,
            RepeatingEventsExtendable
        )
        companion object {
            fun typeFromId(id: Long): ExtensionSyncableType<Extendable>? = when(id) {
                Streak.id -> Streak
                FavoriteContacts.id -> FavoriteContacts
                RepeatingEvents.id -> RepeatingEvents
                else -> null
            }
        }
    }
    interface Extendable {
        fun specificsToString(): String
    }
    interface ExtendableCompanion<out T: Extendable> {
        fun fromString(data: String): T
    }
    companion object: ServerSyncableCompanion<ExtensionSyncable> {
        override fun fromJSON(json: JSONObject): ExtensionSyncable {
            val type = ExtensionSyncableType.typeFromId(json.getId()) ?: error("Received unknown extension syncable")
            return ExtensionSyncable(
                type,
                type.parser.fromString(json.getString("data"))
            )
        }
    }
}