package com.myxoz.life.api.syncables

import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.banking.TransactionSplitEntity
import com.myxoz.life.dbwrapper.banking.TransactionSplitPartEntity
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.jsonObjArray
import org.json.JSONArray
import org.json.JSONObject

data class TransactionSplitSyncable(
    override val id: Long,
    val syncableId: Long?,
    val remoteId: String?,
    val parts: List<Part>
): Syncable(SpecialSyncablesIds.TRANSACTIONSPLIT, id) {
    val key = syncableId to remoteId

    override suspend fun specificsToJson(): JSONObject? = JSONObject()
        .put("syncable_id", syncableId?: JSONObject.NULL)
        .put("remote_id", remoteId?: JSONObject.NULL)
        .put("parts", JSONArray().apply {
            for (part in parts) {
                put(part.toJSON())
            }
        })

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.bankingDao.insertSplitAndParts(
            TransactionSplitEntity(id,syncableId,remoteId),
            parts.map { it.toEntity(id) }
        )
    }

    fun ensureSynced() = if(isSynced()) this else copy(id = API.generateId())

    companion object: ServerSyncableCompanion<TransactionSplitSyncable> {
        override fun fromJSON(json: JSONObject): TransactionSplitSyncable =
            TransactionSplitSyncable(
                json.getId(),
                json.getLongOrNull("syncable_id"),
                json.getStringOrNull("remote_id"),
                json.getJSONArray("parts").jsonObjArray.map {
                    Part(it.getString("person").toLong(), it.getInt("amount"))
                }
            )
        data class Part(val person: Long, val amount: Int) {
            fun toJSON(): JSONObject = JSONObject()
                .put("person", person)
                .put("amount", amount)
            fun toEntity(id: Long) = TransactionSplitPartEntity(id, person, amount)
            companion object {
                fun from(entity: TransactionSplitPartEntity) = Part(entity.person, entity.amount)
            }
        }
    }
}