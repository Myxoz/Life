package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.api.extensions.FavoriteContactsExtendable
import com.myxoz.life.storage.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.storage.api.extensions.StreakExtendable

data class ExtensionSyncable(
    val type: ExtensionSyncableType<Extendable>,
    val content: Extendable
): Syncable {
    override val id: Long = type.id
    sealed class ExtensionSyncableType<out T: Extendable>(
        val id: Long,
        internal val parser: ExtendableCompanion<T>
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
        internal companion object {
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
    internal interface ExtendableCompanion<out T: Extendable> {
        fun fromString(data: String): T
    }
}