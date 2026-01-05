package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncable
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.StorageManager
import org.json.JSONObject

class BankingSyncable : ServerSyncable(Syncable.SpecialSyncablesIds.BANKING) {
    companion object : ServerSyncableCompanion {
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            db.banking.insertTransaction(
                BankingEntity(
                    it.getString("id"),
                    it.getString("card") == "1",
                    it.getString("transfer") == "1",
                    it.getString("amount_cents").toInt(),
                    it.getString("currency"),
                    it.getString("booking_time"),
                    it.getString("value_date").toLong(),
                    it.getString("from_name"),
                    it.getString("from_iban"),
                    it.getString("from_bic"),
                    it.getString("purpose"),
                    it.getString("saldo_after_cents").toLong(),
                    it.getString("purpose_date").toLongOrNull(),
                    it.getString("last_update").toLong()
                )
            )
        }
    }
}