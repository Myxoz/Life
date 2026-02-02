package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.banking.BankingSidecarEntity
import com.myxoz.life.api.API
import com.myxoz.life.screens.feed.fullscreenevent.getId
import org.json.JSONObject

class BankingSidecarSyncable(
    id: Long,
    val transactionId: String,
    val name: String,
    val date: Long,
) : Syncable(SpecialSyncablesIds.BANKINGSIDECAR, id) {
    override suspend fun specificsToJson(): JSONObject? = JSONObject()
        .put("transactionId", transactionId)
        .put("name", name)
        .put("date", date)

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.bankingDao.insertSidecar(
            BankingSidecarEntity(id, transactionId, name, date)
        )
    }

    companion object : ServerSyncableCompanion<BankingSidecarSyncable> {
        override fun fromJSON(json: JSONObject): BankingSidecarSyncable = BankingSidecarSyncable(
            json.getId(),
            json.getString("transactionId"),
            json.getString("name"),
            json.getLong("date")
        )

        override suspend fun fromDB(
            db: API.ReadSyncableDaos,
            id: Long
        ): BankingSidecarSyncable? {
            val sidecar = db.bankingDao.getSidecar(id) ?: return null
            return BankingSidecarSyncable(
                id,
                sidecar.transactionId,
                sidecar.name,
                sidecar.date,
            )
        }
    }
}