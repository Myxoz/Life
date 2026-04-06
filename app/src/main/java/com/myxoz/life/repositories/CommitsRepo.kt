package com.myxoz.life.repositories

import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.repositories.utils.PerformantCache
import com.myxoz.life.repositories.utils.PerformantInterlockedCache
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
    private val _repos = PerformantCache<String, CommitSyncable>(appScope) {
        error("Why tf was this called? If this errors you really did a lot wrong. Fetching a repo by key eventhough we dont even know the Repo, props. Ah and btw, you attempted to fetch $it.")
    }
    val getAllRepos = _repos.allValuesFlow.map{ it.sortedByDescending { repo -> repo.commitDate ?: 0L } }

    private val _commits = PerformantInterlockedCache.dayedSame<String, CommitSyncable>(
        appScope,
        {
            listOfNotNull(it.commitDate?.toLocalDate(zone))
        },
        { first, other -> first.commitSha == other.commitSha },
        { key  ->
            CommitSyncable.fromDB(readSyncableDaos.commitsDao, key)
        },
        {from, to ->
            readSyncableDaos.commitsDao.getCommitsForDay(
                from.atStartAsMillis(zone),
                to.atEndAsMillis(zone),
            ).map { entity ->
                entity.commitSha to CommitSyncable.from(entity)
            }
        }
    ) { key, new ->
        // We do not expect a GitHub sha to double, else they would have
        // a big problem and the app would just need to be restarted
        if (!_repos.hasCached(new.toRepoKey())) { // We got a commit for a brand-new repo
            _repos.overwrite(new.toRepoKey(), new) // Make this commit represent the repo
        } else {
            val old = _repos.getCached(new.toRepoKey()) ?: return@dayedSame
            if((old.commitDate ?: 0L) < (new.commitDate ?: 0L))
                _repos.overwrite(new.toRepoKey(), new)
                // We got a new commit which is more recent than the last one. Replace!
        }
    }
    fun getCommitsForDay(date: LocalDate) = _commits.getInterlockedFlowFor(date)

    fun getAllCommitsFor(repoName: String) = _commits.allValuesFlow.map { commit -> commit.filter { it.repoName == repoName } }
    fun updateCommit(commitSyncable: CommitSyncable) {
        _commits.overwrite(commitSyncable.commitSha, commitSyncable)
    }
    fun getCommit(sha: String) = _commits.flowFor(sha)
    init {
        appScope.launch {
            _commits.markAllEntriesAsLoaded()
            _commits.overwriteAll(
                readSyncableDaos.commitsDao.getAllCommitsEver().map {
                    it.commitSha to CommitSyncable.from(it)
                }
            )
        }
    }
}