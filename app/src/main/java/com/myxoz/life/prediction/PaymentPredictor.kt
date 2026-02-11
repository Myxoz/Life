package com.myxoz.life.prediction

import android.content.Context
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.prediction.Normalize.lnUnsigned
import com.myxoz.life.prediction.Normalize.putTime
import com.myxoz.life.prediction.Normalize.stdDev
import com.myxoz.life.prediction.PredictionFileSystem.getPathOfModel
import com.myxoz.life.prediction.PredictionFileSystem.readLongList
import com.myxoz.life.prediction.PredictionFileSystem.readStringList
import com.myxoz.life.prediction.PredictionFileSystem.writeLongList
import com.myxoz.life.prediction.PredictionFileSystem.writeStringList
import com.myxoz.life.utils.def
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

class PaymentPredictor {
    class PaymentBuilder(
        val mean: Double,
        val stdDev: Double,
        val travelBuckets: Map<Long, Int>
    ) {
        val featureSize = 4 /* Date */ + 1 /* Amount */ + travelBuckets.size
        fun buildFeatures(payment: ReadBankingDao.BankingTrainingRow): DoubleArray {
            val vector = DoubleArray(featureSize)
            var index = 0
            index += vector.putTime(index, payment.purposeDate)
            val logAmount = payment.amountCents.toDouble().lnUnsigned()
            vector[index++] = (logAmount - mean) / stdDev

            val ibanIndex = travelBuckets[payment.travelLocationId] ?: -1
            if (ibanIndex >= 0) {
                vector[index + ibanIndex] = 1.0
            }

            index += travelBuckets.size
            return vector
        }
    }
    class PaymentModel(
        private val paymentBuilder: PaymentBuilder,
        private val model: HotOnePrediction,
        val buckets: List<String>
    ) {
        fun getPredictedNameFor(entry: ReadBankingDao.BankingTrainingRow): String{
            return buckets[model.predict(paymentBuilder.buildFeatures(entry))]
        }
//        fun getPredictedNameProbsFor(entry: ReadBankingDao.BankingTrainingRow): DoubleArray {
//            return model.predictProbFor(paymentBuilder.buildFeatures(entry))
//        }
        fun save(context: Context){
            return DataOutputStream(BufferedOutputStream(FileOutputStream(getPathOfModel(context, "payments")))).use { out ->
                out.writeStringList(buckets)
                out.writeDouble(paymentBuilder.mean)
                out.writeDouble(paymentBuilder.stdDev)
                out.writeLongList(paymentBuilder.travelBuckets.entries.sortedBy { it.value }.map { it.key })
                model.saveModelToFile(out)
            }
        }
        fun train(dataset: List<ReadBankingDao.BankingTrainingRow>){
            val mapedBuckets = buckets.withIndex().associate { it.value to it.index }
            val mapedSet = dataset.map { paymentBuilder.buildFeatures(it) to mapedBuckets.getValue(it.fromName) }
            repeat(1){
                for((x, label) in mapedSet) {
                    model.train(x, label)
                }
            }
        }
    }
    companion object {
        private suspend fun getModel(db: ReadBankingDao): PaymentModel{
            val dataset = db.getBankingTrainingRows()
            val buckets = dataset.map { it.fromName }.distinct()
            val travelBuckets = dataset.map { it.travelLocationId }.distinct().withIndex().associate { it.value.def(0) to it.index  }
            val logAmounts = dataset.map { it.amountCents.toDouble().lnUnsigned() }
            val builder = PaymentBuilder(
                logAmounts.average(),
                logAmounts.stdDev(),
                travelBuckets
            )
            val model = HotOnePrediction(buckets.size, builder.featureSize)
            return PaymentModel(builder, model, buckets)
        }
        suspend fun getModel(context: Context, db: ReadBankingDao): PaymentModel {
            val modelPath = getPathOfModel(context, "payments")
            if(!modelPath.exists()) {
                return getModel(db).apply { save(context) }
            }
            return DataInputStream(BufferedInputStream(FileInputStream(modelPath))).use { inp ->
                val buckets = inp.readStringList()
                val mean = inp.readDouble()
                val stdDev = inp.readDouble()
                val travelBuckets = inp.readLongList().withIndex().associate { it.value to it.index }
                val builder = PaymentBuilder(
                    mean,
                    stdDev,
                    travelBuckets
                )
                PaymentModel(
                    builder,
                    HotOnePrediction.loadModelFromFile(inp),
                    buckets,
                )
            }
        }
    }
}