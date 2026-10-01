package com.myxoz.life.storage.api.syncables

data class DeleteEntry(val deleteCalendarId: Int, override val id: Long): Syncable