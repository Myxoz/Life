package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.StateFlowCache
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import kotlinx.coroutines.flow.map

class CommitsViewModel(private val repos: AppRepositories): ViewModel() {
    private val commitCache = StateFlowCache<String, CommitSyncable?>{ sha ->
        repos.commitsRepo.getCommit(sha).map { it?.data }.subscribeToColdFlow(viewModelScope, null)
    }
    fun getCommit(sha: String) = commitCache.get(sha)

    private val commitsForCache = StateFlowCache<String, List<CommitSyncable>>{ reponame ->
        repos.commitsRepo.getAllCommitsFor(reponame).map {
            it.sortedByDescending { commit -> commit.commitDate?:0L }
        }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getAllCommitsFor(name: String) = commitsForCache.get(name)
    val getAllRepos = repos.commitsRepo.getAllRepos.subscribeToColdFlow(viewModelScope, listOf())
}