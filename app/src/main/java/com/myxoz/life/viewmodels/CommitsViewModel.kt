package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.FlowCache
import kotlinx.coroutines.flow.map

class CommitsViewModel(private val repos: AppRepositories): ViewModel() {
    private val commitCache = FlowCache<String, CommitSyncable?>{ sha ->
        repos.commitsRepo.getCommit(sha).map { it?.data }
    }
    fun getCommit(sha: String) = commitCache.get(sha)

    private val commitsForCache = FlowCache<String, List<CommitSyncable>>{ reponame ->
        repos.commitsRepo.getAllCommitsFor(reponame).map {
            it.sortedByDescending { commit -> commit.commitDate?:0L }
        }
    }
    fun getAllCommitsFor(name: String) = commitsForCache.get(name)
    val getAllRepos = repos.commitsRepo.getAllRepos
}