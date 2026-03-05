package com.myxoz.life.api.syncables

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.myxoz.life.aggregator.CalendarAggregator
import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.banking.ManualTransactionEntity
import com.myxoz.life.repositories.BankingRepo
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.utils.getServerBoolean
import com.myxoz.life.utils.getStringOrNull
import org.json.JSONObject

data class ManualTransactionSyncable(
    override val id: Long,
    val digital: Boolean,
    val cashless: Boolean,
    val amountCents: Int,
    val name: String,
    override val timestamp: Long,
    val purpose: String?
): Syncable(SpecialSyncablesIds.MANUALTRANSACTION, id), Syncable.DatedSyncable {
    override suspend fun specificsToJson(): JSONObject? =
        JSONObject()
            .put("id", id.toString())
            .put("digital", digital)
            .put("cashless", cashless)
            .put("amount", amountCents)
            .put("name", name)
            .put("timestamp", timestamp.toString())
            .put("purpose", purpose)

    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.bankingDao.insertManualTransaction(
            ManualTransactionEntity(id, digital, cashless, amountCents, name, timestamp, purpose)
        )
    }

    fun asInstantEvent() = CalendarAggregator.bankEntryAsInstantEvent(BankingRepo.BankingDisplayEntity.from(this))

    fun ensureSynced() = if(id == -1L) copy(id = API.generateId()) else this

    override fun getInvalidReason(): String? = when{
        name.isEmpty() -> "Gib einen Namen ein"
        amountCents == 0 -> "Gib einen Betrag ein, der nicht 0 ist"
        else -> null
    }

    override fun copyWithTs(timestamp: Long) = copy(timestamp = timestamp)

    companion object: ServerSyncableCompanion<ManualTransactionSyncable> {
        override fun fromJSON(json: JSONObject): ManualTransactionSyncable =
            ManualTransactionSyncable(
                json.getId(),
                json.getServerBoolean("digital"),
                json.getServerBoolean("cashless"),
                json.getInt("amount"),
                json.getString("name"),
                json.getLong("timestamp"),
                json.getStringOrNull("purpose"),
            )
        fun fromEntity(entity: ManualTransactionEntity): ManualTransactionSyncable =
            ManualTransactionSyncable(
                entity.id,
                entity.digital,
                entity.cashless,
                entity.amountCents,
                entity.name,
                entity.timestamp,
                entity.purpose
            )
    }
    class MoneyBasedVisualTransformation: VisualTransformation {
        override fun filter(text: AnnotatedString): TransformedText {
            val anotatedText = toTransformed(text.text)
            return TransformedText(
                AnnotatedString(anotatedText),
                MoneyBasedOffsetMapping(anotatedText, text.text)
            )
        }
        class MoneyBasedOffsetMapping(val transformed: String, val original: String): OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                // 1005
                // to 10,05 €
                val dist = original.length - offset
                val res = transformed.length - when(dist){ // 0  is at the end
                    0 -> 2
                    1 -> 3
                    else -> dist + 3
                }
                return res
            }

            override fun transformedToOriginal(offset: Int): Int {
                // 10,05 €
                // to 1005
                val dist = transformed.length - offset
                val res = (original.length - when(dist){ // 0  is at the end
                    0 -> 0 // 10,05 €| -> 1005|
                    1 -> 0 // 10,05 |€ -> 1005|
                    2 -> 0 // 10,05| € -> 1005|
                    3 -> 1 // 10,0|5 € -> 100|5
                    4 -> 2 // 10,|05 € -> 10|05
                    else -> dist - 3
                }).coerceIn(0, original.length) // Bad practice-ish
                return res
            }
        }
        companion object {
            fun toTransformed(text: String): String{
                return if(text.length > 2) "${text.take(text.length-2)},${text.takeLast(2)} €" else "0,${text.takeLast(2).padStart(2,'0')} €"
            }
            fun MoneyBasedOffsetMapping.testTransformed(res: Int, offset: Int){
                println("${transformed.take(offset)}|${transformed.substring(offset)} -> ${original.take(res)}|${original.substring(res)}")
            }
            fun MoneyBasedOffsetMapping.testOriginal(res: Int, offset: Int){
                println("${original.take(offset)}|${original.substring(offset)} -> ${transformed.take(res)}|${transformed.substring(res)}")
            }
        }
    }
}