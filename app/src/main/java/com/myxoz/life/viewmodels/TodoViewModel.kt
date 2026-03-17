package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.map

class TodoViewModel(repos: AppRepositories): ViewModel() {
    private val _cache = StateFlowCache<Long, TodoSyncable?>{ id ->
        repos.todoRepo.getTodo(id).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getTodo(id: Long) = _cache.get(id)
}