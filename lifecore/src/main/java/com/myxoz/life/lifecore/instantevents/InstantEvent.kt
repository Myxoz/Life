package com.myxoz.life.lifecore.instantevents

import android.app.AlarmManager
import androidx.annotation.DrawableRes
import com.myxoz.life.core.toInstant
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifecore.android.sendWithBal
import com.myxoz.life.presentation.BankingFormating.formatCents
import com.myxoz.life.presentation.BankingFormating.getIcon
import com.myxoz.life.presentation.R
import com.myxoz.life.presentation.events.InstantEventFormatable.ALARMCLOCKTITLE
import com.myxoz.life.storage.api.helper.MixedBankingEntity
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.api.syncables.TodoSyncable
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
sealed class InstantEvent(
    val timestamp: Instant,
    @DrawableRes val icon: Int,
    val subText: String,
    val openInstantEvent: InstantEventOpener.() -> Unit
) {
    @Serializable
    class AlarmClock(alarmInfo: AlarmManager.AlarmClockInfo) : InstantEvent(
        alarmInfo.triggerTime.toInstant(),
        R.drawable.alarm_clock,
        ALARMCLOCKTITLE,
        {
            alarmInfo.showIntent.sendWithBal()
        }
    )

    @Serializable
    class Todo(todoSyncable: TodoSyncable) : InstantEvent(
        todoSyncable.timestamp,
        R.drawable.todos_icon,
        todoSyncable.short,
        {
            openRoute(NavPath.Menu.Todo.SpecificTodo(todoSyncable.id))
        }
    )

    @Serializable
    class Transaction(transaction: MixedBankingEntity, displayTimestamp: Instant) : InstantEvent(
        displayTimestamp,
        transaction.getIcon(),
        transaction.amount.formatCents(),
        {
            openRoute(NavPath.Transactions.Details(transaction.key))
        }
    )

    @Serializable
    class Commit(commitSyncable: CommitSyncable, commitTimestamp: Instant) : InstantEvent(
        commitTimestamp,
        R.drawable.commit,
        commitSyncable.repoName,
        {
            openRoute(NavPath.Menu.Repos.Commit(commitSyncable.commitSha))
        }
    )
}