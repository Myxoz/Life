package com.myxoz.life.repositories

import androidx.lifecycle.ViewModel
import com.myxoz.life.storage.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.storage.interfaces.DatabaseInterface

class AISettingsRepo(val repos: DatabaseInterface): ViewModel() {
    suspend fun resetPaymentPredictorModel(){
        repos.aiPredictionRepo.resetPaymentPredictorModel()
    }
    suspend fun getTrainingData(): List<ReadBankingDao.BankingTrainingRow> {
        return repos.aiPredictionRepo.fetchPaymentTrainingData()
    }
    fun trainPayments(dataset: List<ReadBankingDao.BankingTrainingRow>, times: Int, onRun: (Int) -> Unit){
        repeat(times){
            repos.aiPredictionRepo.paymentPredictor?.train(dataset)
            onRun(it)
        }
        repos.aiPredictionRepo.paymentPredictor?.save(repos.context)
    }
}