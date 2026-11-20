package com.myxoz.life.state

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myxoz.life.feature.socialgraph.SocialGraphRepo
import com.myxoz.life.state.activityscoped.AISettingsRepo
import com.myxoz.life.state.activityscoped.BirthdayQuizRepo
import com.myxoz.life.state.activityscoped.ContactsRepo
import com.myxoz.life.state.activityscoped.LargeDataCache
import com.myxoz.life.state.activityscoped.StreakRepo
import com.myxoz.life.state.activityscoped.TransportRepo
import com.myxoz.life.state.activityscoped.supermodule.CalendarSuper
import com.myxoz.life.state.activityscoped.supermodule.CrossRepoSuper
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.alarm.AlarmRepo
import com.myxoz.life.ui.feed.CalendarRepo
import com.myxoz.life.ui.feed.main.CalendarApplicationState
import com.myxoz.life.ui.map.MapRepo
import com.myxoz.life.ui.transactions.TransactionFeedRepo
import com.myxoz.life.ui.wrapped.WrappedActivityViewModel

class ActivityScopedState(dbInterface: DatabaseInterface, val activity: ComponentActivity) {
    private val factory = viewModelFactory {
        initializer { WrappedActivityViewModel(dbInterface) }
        initializer { TransportRepo() }
        initializer { CalendarSuper() }
    }
    val wrappedActivityViewModel: WrappedActivityViewModel by activity.viewModels { factory }
    val transportRepo: TransportRepo by activity.viewModels { factory }
    val calendarSuper: CalendarSuper by activity.viewModels { factory }
    val crossRepoSuper by CrossRepoSuper(dbInterface)
    val largeDataCache = LargeDataCache()

    val alarmRepo = AlarmRepo(dbInterface.aggregators.alarmAggregators)
    val calendarRepo = CalendarRepo(dbInterface, calendarSuper)
    val calendarApplicationState = CalendarApplicationState(dbInterface)
    val transactionFeedRepo = TransactionFeedRepo(dbInterface)
    val socialGraphRepo = SocialGraphRepo(dbInterface)
    val birthdayQuizRepo = BirthdayQuizRepo(dbInterface)
    val contactsRepo = ContactsRepo(dbInterface)
    val aiSettingsRepo = AISettingsRepo(dbInterface)
    val streakRepo = StreakRepo(dbInterface)
    val mapRepo: MapRepo = MapRepo(dbInterface)
}