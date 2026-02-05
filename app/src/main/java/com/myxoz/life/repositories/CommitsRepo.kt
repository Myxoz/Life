package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.repositories.utils.VersionedCache
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class CommitsRepo(
    private val readSyncableDaos: API.ReadSyncableDaos,
    private val appScope: CoroutineScope
) {
    private val zone = ZoneId.systemDefault()
    private val _repos = VersionedCache<String, CommitSyncable>(
        {
            throw Error("Why tf was this called? If this errors you really did a lot wrong. Fetching a repo by key eventhough we dont even know the Repo, props")
        }
    )
    val getAllRepos = _repos.allValuesFlow().map{ it.map { repo -> repo.data }.sortedByDescending { repo -> repo.commitDate ?: 0L } }

    private val _commitsDayed = VersionedCache<LocalDate, List<CommitSyncable>>(
        { date ->
            readSyncableDaos.commitsDao.getCommitsForDay(
                date.atStartAsMillis(zone),
                date.atEndAsMillis(zone),
            ).map { entity ->
                CommitSyncable.from(entity)
            }
        }
    )
    fun getCommitsForDay(date: LocalDate) = _commitsDayed.flowByKey(appScope, date)

    private val _commits = VersionedCache<String, CommitSyncable>(
        { sha ->
            CommitSyncable.fromDB(readSyncableDaos.commitsDao, sha)
        }
    ) { _, old, new ->
        // We do not expect a Github sha to double, else they would have
        // a big problem and the app would just need to be restarted
        if (!_repos.hasKey(new.toRepoKey())) { // We got a commit for a brand new repo
            _repos.overwrite(new.toRepoKey(), new) // Make this commit represent the repo
        } else {
            val old = _repos.get(new.toRepoKey())
            if((old.data.commitDate ?: 0L) < (new.commitDate ?: 0L))
                _repos.overwrite(new.toRepoKey(), new)
                // We got a new commit which is more recent than the last one. Replace!
        }
        if (old?.commitDate != null) {
            val commitDate = old.commitDate.toLocalDate(zone)
            _commitsDayed.updateWith(commitDate) {
                it.filterNot { commit ->
                    commit.commitSha == old.commitSha
                }
            }
            if (new.commitDate != null) {
                val commitDate = new.commitDate.toLocalDate(zone)
                _commitsDayed.updateWith(commitDate){ list ->
                    list + new
                }
            }
        }
    }
    fun getAllCommitsFor(repoName: String) = _commits.allValuesFlow().map { commit -> commit.filter { it.data.repoName == repoName }.map { it.data } }
    suspend fun updateCommit(commitSyncable: CommitSyncable) {
        _commits.overwrite(commitSyncable.commitSha, commitSyncable)
    }
    fun getCommit(sha: String) = _commits.flowByKey(appScope, sha)
    init {
        appScope.launch {
            _commits.overwriteAll(
                readSyncableDaos.commitsDao.getAllCommitsEver().map {
                    it.commitSha to CommitSyncable.from(it)
                }
            )
        }
    }
}