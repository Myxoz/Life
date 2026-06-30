package com.myxoz.life.ui.todo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import com.myxoz.life.ui.NavPath

class TodoViewModel(
    savedStateHandle: SavedStateHandle,
    repos: DatabaseInterface
): ViewModel() {
    val id = savedStateHandle.get<Long>(NavPath.Menu.Todo.DETAILS.parameterName) ?: error("Cannot parse todo id")
    val todo = repos.todoInterface.getTodo(id).subscribeToColdFlow(viewModelScope, null)
}