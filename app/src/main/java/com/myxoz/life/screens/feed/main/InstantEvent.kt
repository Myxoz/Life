package com.myxoz.life.screens.feed.main

import androidx.core.content.edit
import androidx.navigation.NavController
import com.myxoz.life.R
import com.myxoz.life.dbwrapper.BankingEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.dbwrapper.formatCents
import com.myxoz.life.viewmodels.CalendarViewModel
import org.json.JSONObject

class InstantEvent(
    val icon: Int,
    val subText: String,
    val timestamp: Long,
    val openDetails: (NavController)->Unit
) {
    companion object {
        suspend fun getEntriesForDay(db: StorageManager, startOfDay: Long, endOfDay: Long, viewModel: CalendarViewModel): List<InstantEvent> {
            val entries = BankingEntity.getAllBankingEntriesFor(db, startOfDay, endOfDay, viewModel.futureBankEntries).mapNotNull {
                InstantEvent(
                    if(it.card) R.drawable.pay_with_card else R.drawable.gpay,
                    it.amountCents.formatCents(),
                    it.purposeDate?:return@mapNotNull null,
                ) { nav ->
                    if(it.id.isBlank()) db.prefs.edit {
                        putString(
                            "transactionAtHand", JSONObject()
                                .put("amount", it.amountCents)
                                .put("timestamp", it.purposeDate)
                                .put("to", it.fromName)
                                .toString()
                        )
                    }
                    nav.navigate("bank/transaction/${it.id}")
                }
            }
            val commits = db.commits.getCommitsForDay(startOfDay, endOfDay).mapNotNull {
                InstantEvent(
                    R.drawable.commit,
                    it.repoName,
                    it.commitDate?:return@mapNotNull null,
                ) { nav ->
                    nav.navigate("commits/commit/${it.commitSha}")
                }
            }
            val mix = entries + commits
            return mix
        }
    }
}