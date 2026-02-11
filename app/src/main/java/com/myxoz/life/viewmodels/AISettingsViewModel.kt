package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import com.myxoz.life.dbwrapper.banking.ReadBankingDao
import com.myxoz.life.repositories.AppRepositories

class AISettingsViewModel(val repos: AppRepositories): ViewModel() {
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