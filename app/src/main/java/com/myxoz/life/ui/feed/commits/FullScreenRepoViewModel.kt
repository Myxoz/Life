package com.myxoz.life.ui.feed.commits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.myxoz.life.storage.interfaces.CommitsInterface
import com.myxoz.life.ui.NavPath

class FullScreenRepoViewModel(
    dbInterface: CommitsInterface,
    savedStateHandle: SavedStateHandle
): ViewModel() {
    val repoName: String = savedStateHandle.get<String>(NavPath.Menu.Repos.REPO.parameterName)!!
    val allCommits = dbInterface.getAllCommitsFor(repoName)
}