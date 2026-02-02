package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncable
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.banking.BankingEntity
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.api.API
import org.json.JSONObject

class BankingSyncable(
    val entity: BankingEntity
) : ServerSyncable(Syncable.SpecialSyncablesIds.BANKING) {
    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.bankingDao.insertTransaction(entity)
    }

    suspend fun getTransactionDate(db: ReadBankingDao): Long =
        getTransactionDate(db.getSidecar(entity.id)?.date)
    fun getTransactionDate(sidecarTs: Long?): Long =
         sidecarTs ?: entity.purposeDate ?: entity.valueDate

    companion object : ServerSyncableCompanion<BankingSyncable> {
        override fun fromJSON(json: JSONObject): BankingSyncable =
            BankingSyncable(
                BankingEntity(
                    json.getString("id"),
                    json.getString("card") == "1",
                    json.getString("transfer") == "1",
                    json.getString("amount_cents").toInt(),
                    json.getString("currency"),
                    json.getString("booking_time"),
                    json.getString("value_date").toLong(),
                    json.getString("from_name"),
                    json.getString("from_iban"),
                    json.getString("from_bic"),
                    json.getString("purpose"),
                    json.getString("saldo_after_cents").toLong(),
                    json.getString("purpose_date").toLongOrNull(),
                    json.getString("last_update").toLong()
                )
            )
    }
}