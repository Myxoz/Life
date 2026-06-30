package com.myxoz.life.api

import com.myxoz.life.api.extensions.RepeatingEventsExtendable
import com.myxoz.life.api.syncables.ExtensionSyncable
import com.myxoz.life.api.syncables.ExtensionSyncable.ExtensionSyncableType.FavoriteContacts
import com.myxoz.life.api.syncables.ExtensionSyncable.ExtensionSyncableType.RepeatingEvents
import com.myxoz.life.api.syncables.ExtensionSyncable.ExtensionSyncableType.Streak
import com.myxoz.life.events.LocalEvent
import com.myxoz.life.storage.interfaces.CalendarInterface
import com.myxoz.life.storage.interfaces.ExtensionInterface
import com.myxoz.life.storage.interfaces.utils.Cached

/**
 * We try to derive everything, but sometimes this is not possible without duplicating code.
 * Out Layout looks like this:
 *
 *
 * REPOS
 *
 * {HOOKS (HERE)}
 *
 * [API]
 *
 * VIEWMODELS
 *
 * UI
 *
 *
 * Most parts fit into there but when we e.x. on Extendable update, update all caches as well, we run
 * into the problem that the API should have access to this as well (else duplicate code), but it
 * needs to be able to update repos, so it needs to lie below repos and above or in the API, but I
 * do not wanna trash the API file wiht specific hooks for every kind of update. So this file was
 * created.
 * */
class UpdateHooks(
    val calendarInterface: CalendarInterface,
    val extensionInterface: ExtensionInterface
) {
    suspend fun updateExtension(extension: ExtensionSyncable) {
        when(extension.type) {
            Streak,
            FavoriteContacts -> return
            RepeatingEvents -> updateRepeatingEvents(extension.content as? RepeatingEventsExtendable ?: return)
        }
    }
    suspend fun updateRepeatingEvents(repeatingEvents: RepeatingEventsExtendable) {
        val newEvents = repeatingEvents.events

        // Whoa whoa whoa! You're just recycling the RepeatingEvent id's as local ids?
        // Yes I am, think about it, it may seam weird but, we don't harm anything and
        // allow easiest updating when proposing multiple LocalEvents.
        // In the future, we will likely switch to a similar approach just with adding or similar
        calendarInterface.updateLocalEvents(newEvents.map { LocalEvent(it.id, it.event) })

        val old = extensionInterface.getExtension(RepeatingEvents)
        if(old != null) {
            val oldIds = old.events.map { it.id }
            val difference = (oldIds - newEvents.map { it.id }.toSet())
            // Most likely: Only edited not changed
            if(difference.isEmpty()) return

            calendarInterface.updateLocalEventsRaw( difference.map { it to Cached.Null } )
        }
    }
}