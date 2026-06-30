package com.myxoz.life.ui.feed.commits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.myxoz.life.storage.interfaces.CommitsInterface
import com.myxoz.life.ui.NavPath

class FullScreenCommitViewModel(
    dbInterface: CommitsInterface,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val sha: String = savedStateHandle.get<String>(NavPath.Menu.Repos.COMMIT.parameterName)!!
    val commit = dbInterface.getCommit(sha)
}