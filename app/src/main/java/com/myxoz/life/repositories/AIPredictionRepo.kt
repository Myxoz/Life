package com.myxoz.life.repositories

import android.content.Context
import com.myxoz.life.api.API
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.prediction.PaymentPredictor
import com.myxoz.life.prediction.PredictionFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIPredictionRepo(
    private val applicationContext: Context,
    val readSyncableDaos: API.ReadSyncableDaos,
    val appScope: CoroutineScope
) {
    var paymentPredictor: PaymentPredictor.PaymentModel? = null
    fun predictPaymentName(entity: ReadBankingDao.BankingTrainingRow): String? {
        return paymentPredictor?.getPredictedNameFor(entity)
    }
//    fun predictPaymentProbs(entity: ReadBankingDao.BankingTrainingRow): DoubleArray? {
//        return paymentPredictor?.getPredictedNameProbsFor(entity)
//    }
    suspend fun resetPaymentPredictorModel(){
        PredictionFileSystem.getPathOfModel(applicationContext, "payments").delete()
        paymentPredictor = PaymentPredictor.getModel(applicationContext, readSyncableDaos.bankingDao)
    }
    fun trainModel(dataset: List<ReadBankingDao.BankingTrainingRow>){
        paymentPredictor?.train(dataset)
    }
    suspend fun fetchPaymentTrainingData() = readSyncableDaos.bankingDao.getBankingTrainingRows()
    init {
        appScope.launch {
            withContext(Dispatchers.IO) {
                paymentPredictor = PaymentPredictor.getModel(
                    applicationContext,
                    readSyncableDaos.bankingDao
                )
            }
        }
    }
}