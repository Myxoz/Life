package com.myxoz.life.api.syncables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.aggregator.CalendarAggregator
import com.myxoz.life.api.API
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.banking.ManualTransactionEntity
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.screens.feed.fullscreenevent.getId
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.def
import com.myxoz.life.utils.getServerBoolean
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.nullIfEmpty
import com.myxoz.life.utils.rippleClick
import org.json.JSONObject
import kotlin.math.abs

data class ManualTransactionSyncable(
    override val id: Long,
    val digital: Boolean,
    val cashless: Boolean,
    val amountCents: Int,
    val name: String,
    override val timestamp: Long,
    val purpose: String?
): Syncable(SpecialSyncablesIds.MANUALTRANSACTION, id), Syncable.FeedInstantEventSyncable {
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

    override fun asInstantEvent() = CalendarAggregator.manualTransactionEntityAsInstantEvent(this)

    fun ensureSynced() = if(!isSynced()) copy(id = API.generateId()) else this

    override suspend fun delete(repos: AppRepositories) {
        repos.bankingRepo.deleteManualTransaction(this)
    }

    override suspend fun saveWithCache(repos: AppRepositories) {
        repos.bankingRepo.putManualTransaction(this)
    }

    override fun getFeedInvalidReason(): String? = when{
        name.isEmpty() -> "Gib einen Namen ein"
        amountCents == 0 -> "Gib einen Betrag ein, der nicht 0 ist"
        else -> null
    }

    override fun getInvalidReason(): String? = getFeedInvalidReason()

    override fun copyWithTs(timestamp: Long) = copy(timestamp = timestamp)

    @Composable
    override fun ModifyEventInputs(setSyncableTo: (FeedInstantEventSyncable) -> Unit) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
            ,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var enteredCents by remember { mutableStateOf(abs(amountCents).takeIf { it != 0 }?.toString().def("")) }
            var isNegative by remember { mutableStateOf(amountCents <= 0) }
            fun updateCents(){
                setSyncableTo(copy(amountCents = enteredCents.toIntOrNull().def(0) * (if(isNegative) -1 else 1)))
            }
            val color by animateColorAsState(if(isNegative) OldColors.Transactions.MINUS else OldColors.Transactions.PLUS)
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(color)
                    .rippleClick{
                        isNegative = !isNegative
                        updateCents()
                    }
                    .aspectRatio(1f)
                    .fillMaxHeight(.5f)
                    .height(0.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(!isNegative, enter = scaleIn(), exit = scaleOut()) {
                    Icon(painterResource(R.drawable.add), "Positive", Modifier.fillMaxSize(), Theme.onPrimaryContainer)
                }
                androidx.compose.animation.AnimatedVisibility(isNegative, enter = scaleIn(), exit = scaleOut()) {
                    Icon(painterResource(R.drawable.minus), "Negative", Modifier.fillMaxSize(), Theme.onPrimaryContainer)
                }
            }
            BasicTextField(
                enteredCents,
                {
                    enteredCents = it
                    updateCents()
                },
                Modifier
                    .weight(1f)
                ,
                singleLine = true,
                textStyle = TypoStyle(color, FontSize.XLARGE, FontFamily.Display).copy(textAlign = TextAlign.End),
                visualTransformation = MoneyBasedVisualTransformation(),
                cursorBrush = SolidColor(color)
            )
        }
        InputField(name.ifEmpty { null }, "Empfänger") {
            setSyncableTo(copy(name = it))
        }
        InputField(purpose?.ifEmpty { null }, "Verwendungszweck") {
            setSyncableTo(copy(purpose = it.nullIfEmpty()))
        }
    }

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