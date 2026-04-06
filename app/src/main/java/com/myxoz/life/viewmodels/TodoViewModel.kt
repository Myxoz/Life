package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.Cached
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow

class TodoViewModel(repos: AppRepositories): ViewModel() {
    private val _cache = StateFlowCache<Long, Cached<TodoSyncable>?>{ id ->
        repos.todoRepo.getTodo(id).subscribeToColdFlow(viewModelScope, null)
    }
    fun getTodo(id: Long) = _cache.get(id)
}