package com.myxoz.life.state.screenscoped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.lifecore.flows.StateFlowCache
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.subscribeToColdFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class CommitsViewModel(private val repos: DatabaseInterface): ViewModel() {
    private val commitCache = StateFlowCache<String, CommitSyncable?>{ sha ->
        repos.commitsInterface.getCommit(sha).subscribeToColdFlow(viewModelScope, null)
    }
    fun getCommit(sha: String) = commitCache.get(sha)

    private val commitsForCache = StateFlowCache<String, List<CommitSyncable>>{ reponame ->
        repos.commitsInterface.getAllCommitsFor(reponame).map {
            it.sortedByDescending { commit -> commit.commitDate?: Instant.EPOCH }
        }.subscribeToColdFlow(viewModelScope, listOf())
    }
    fun getAllCommitsFor(name: String) = commitsForCache.get(name)
    val getAllRepos = repos.commitsInterface.getAllRepos.subscribeToColdFlow(viewModelScope, listOf())
}