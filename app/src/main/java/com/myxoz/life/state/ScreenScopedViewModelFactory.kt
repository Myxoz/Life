package com.myxoz.life.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.myxoz.life.state.activityscoped.AISettingsRepo
import com.myxoz.life.state.activityscoped.BirthdayQuizRepo
import com.myxoz.life.state.activityscoped.ContactsRepo
import com.myxoz.life.state.activityscoped.StreakRepo
import com.myxoz.life.state.screenscoped.CommitsViewModel
import com.myxoz.life.state.screenscoped.LocationEditingViewModel
import com.myxoz.life.state.screenscoped.ProfileInfoModel
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.feed.commits.FullScreenCommitViewModel
import com.myxoz.life.ui.feed.commits.FullScreenRepoViewModel
import com.myxoz.life.ui.feed.dayoverview.DayOverviewTransactionModel
import com.myxoz.life.ui.feed.dayoverview.DayOverviewViewModel
import com.myxoz.life.ui.feed.dayoverview.ScreenTimeOverviewModel
import com.myxoz.life.ui.feed.fullscreenevent.InspectedEventViewModel
import com.myxoz.life.ui.feed.instantevents.InstantEventsViewModel
import com.myxoz.life.ui.person.PersonalDebtViewModel
import com.myxoz.life.ui.todo.TodoViewModel
import com.myxoz.life.ui.transactions.TransactionOverviewViewModel

@Suppress("UNCHECKED_CAST")
class ScreenScopedViewModelFactory(
    private val dbInterfaces: DatabaseInterface,
    private val repos: ActivityScopedState,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val handle = extras.createSavedStateHandle()
        return when {
            modelClass.isAssignableFrom(BirthdayQuizRepo::class.java) ->
                BirthdayQuizRepo(dbInterfaces) as T

            modelClass.isAssignableFrom(StreakRepo::class.java) ->
                StreakRepo(dbInterfaces) as T

            modelClass.isAssignableFrom(TodoViewModel::class.java) ->
                TodoViewModel(handle, dbInterfaces) as T

            modelClass.isAssignableFrom(FullScreenCommitViewModel::class.java) ->
                FullScreenCommitViewModel(dbInterfaces.commitsInterface, handle) as T

            modelClass.isAssignableFrom(FullScreenRepoViewModel::class.java) ->
                FullScreenRepoViewModel(dbInterfaces.commitsInterface, handle) as T

            modelClass.isAssignableFrom(InspectedEventViewModel::class.java) ->
                InspectedEventViewModel(handle, dbInterfaces, repos.calendarSuper) as T

            modelClass.isAssignableFrom(LocationEditingViewModel::class.java) ->
                LocationEditingViewModel(dbInterfaces.locationInterface) as T

            modelClass.isAssignableFrom(DayOverviewViewModel::class.java) ->
                DayOverviewViewModel(dbInterfaces, handle) as T

            modelClass.isAssignableFrom(ScreenTimeOverviewModel::class.java) ->
                ScreenTimeOverviewModel(dbInterfaces, handle) as T

            modelClass.isAssignableFrom(DayOverviewTransactionModel::class.java) ->
                DayOverviewTransactionModel(dbInterfaces.bankingInterface, handle) as T

            modelClass.isAssignableFrom(InstantEventsViewModel::class.java) ->
                InstantEventsViewModel(handle, repos.transportRepo) as T

            modelClass.isAssignableFrom(ProfileInfoModel::class.java) ->
                ProfileInfoModel(handle, dbInterfaces.aggregators.peopleEventAggregator, repos.crossRepoSuper) as T

            modelClass.isAssignableFrom(ContactsRepo::class.java) ->
                ContactsRepo(dbInterfaces) as T

            modelClass.isAssignableFrom(AISettingsRepo::class.java) ->
                AISettingsRepo(dbInterfaces) as T

            modelClass.isAssignableFrom(PersonalDebtViewModel::class.java) ->
                PersonalDebtViewModel(dbInterfaces, handle) as T

            modelClass.isAssignableFrom(TransactionOverviewViewModel::class.java) ->
                TransactionOverviewViewModel(handle, dbInterfaces) as T

            modelClass.isAssignableFrom(CommitsViewModel::class.java) ->
                CommitsViewModel(dbInterfaces) as T

            else -> error("Unknown ViewModel: $modelClass")
        }
    }
}