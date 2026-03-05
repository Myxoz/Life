package com.myxoz.life.api.syncables

import com.myxoz.life.api.API
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.WaitingSyncDao
import com.myxoz.life.dbwrapper.WaitingSyncEntity
import org.json.JSONObject

class DeleteEntry(calendarId: Int, id: Long) : Syncable(calendarId, id) {
    override suspend fun specificsToJson(): JSONObject? =
        JSONObject()
            .put("type", calendarId)
            .put("tags", 1)
            .put("id", id.toString())

    override suspend fun saveToDB(db: API.WriteSyncableDaos) = Unit
    companion object {
        suspend fun requestSyncDelete(waitingSyncDao: WaitingSyncDao, sync: Syncable){
            waitingSyncDao.deleteWaitingSync( // Cancel syncing attempt
                sync.id,
                sync.calendarId
            )
            waitingSyncDao.insertWaitingSync(
                WaitingSyncEntity(sync.id, -sync.calendarId, System.currentTimeMillis())
            )
        }
    }
}