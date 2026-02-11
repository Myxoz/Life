package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.myxoz.life.repositories.AppRepositories

class MainViewModelFactory(
    private val repositories: AppRepositories,
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                CalendarViewModel(repositories) as T

            modelClass.isAssignableFrom(CommitsViewModel::class.java) ->
                CommitsViewModel(repositories) as T

            modelClass.isAssignableFrom(InspectedEventViewModel::class.java) ->
                InspectedEventViewModel(repositories) as T

            modelClass.isAssignableFrom(LocationEditingViewModel::class.java) ->
                LocationEditingViewModel(repositories.locationRepo) as T

            modelClass.isAssignableFrom(TransactionViewModel::class.java) ->
                TransactionViewModel(repositories) as T

            modelClass.isAssignableFrom(DayOverviewViewModel::class.java) ->
                DayOverviewViewModel(repositories) as T

            modelClass.isAssignableFrom(InstantEventsViewModel::class.java) ->
                InstantEventsViewModel() as T

            modelClass.isAssignableFrom(LargeDataCache::class.java) ->
                LargeDataCache() as T

            modelClass.isAssignableFrom(ProfileInfoModel::class.java) ->
                ProfileInfoModel(repositories) as T

            modelClass.isAssignableFrom(ContactsViewModel::class.java) ->
                ContactsViewModel(repositories) as T

            modelClass.isAssignableFrom(SocialGraphViewModel::class.java) ->
                SocialGraphViewModel(repositories) as T

            modelClass.isAssignableFrom(MapViewModel::class.java) ->
                MapViewModel(repositories) as T

            modelClass.isAssignableFrom(AISettingsViewModel::class.java) ->
                AISettingsViewModel(repositories) as T

            else -> error("Unknown ViewModel: $modelClass")
        }
    }
}
