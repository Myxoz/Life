package com.myxoz.life.api

import com.myxoz.life.dbwrapper.StorageManager
import org.json.JSONObject

class DeleteEntry(calendarId: Int, id: Long) : Syncable(calendarId, id) {
    override suspend fun specificsToJson(db: StorageManager): JSONObject =
        JSONObject()
            .put("type", calendarId)
            .put("tags", 1)
            .put("id", id.toString())

    override suspend fun saveToDB(db: StorageManager) = Unit
}