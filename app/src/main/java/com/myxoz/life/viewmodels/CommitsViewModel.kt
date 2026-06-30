package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.storage.interfaces.utils.StateFlowCache
import com.myxoz.life.storage.interfaces.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.map

class CommitsViewModel(private val repos: DatabaseInterface): ViewModel() {
    private val commitCache = StateFlowCache<String, CommitSyncable?>{ sha ->
        repos.commitsInterface.getCommit(sha).subscribeToColdFlow(viewModelScope, null)
    }
    fun getCommit(sha: String) = commitCache.get(sha)

    private val commitsForCache = StateFlowCache<String, List<CommitSyncable>>{ reponame ->
        repos.commitsInterface.getAllCommitsFor(reponame).map {
            it.sortedByDescending { commit -> commit.commitDate?:0L }
        }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getAllCommitsFor(name: String) = commitsForCache.get(name)
    val getAllRepos = repos.commitsInterface.getAllRepos.subscribeToColdFlow(viewModelScope, listOf())
}