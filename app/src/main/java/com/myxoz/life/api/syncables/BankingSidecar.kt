package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.screens.feed.fullscreenevent.getEventId
import com.myxoz.life.dbwrapper.BankingSidecarEntity
import com.myxoz.life.dbwrapper.StorageManager
import org.json.JSONObject

class BankingSidecar(
    id: Long,
    val transactionId: String,
    val name: String,
    val date: Long,
) : Syncable(SpecialSyncablesIds.BANKINGSIDECAR, id) {
    override suspend fun specificsToJson(db: StorageManager): JSONObject? = JSONObject()
        .put("transactionId", transactionId)
        .put("name", name)
        .put("date", date)

    override suspend fun saveToDB(db: StorageManager) {
        db.bankingSidecar.insertSidecar(
            BankingSidecarEntity(id, transactionId, name, date)
        )
    }

    companion object : ServerSyncableCompanion {
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            db.bankingSidecar.insertSidecar(
                BankingSidecarEntity(
                    it.getEventId(),
                    it.getString("transactionId"),
                    it.getString("name"),
                    it.getLong("date")
                )
            )
        }
    }
}