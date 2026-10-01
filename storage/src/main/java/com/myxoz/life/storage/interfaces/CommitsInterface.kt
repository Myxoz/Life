package com.myxoz.life.storage.interfaces

import com.myxoz.life.core.toLocalDate
import com.myxoz.life.storage.api.syncables.CommitSyncable
import com.myxoz.life.storage.caches.InterlockedCache
import com.myxoz.life.storage.caches.PerformantCache
import com.myxoz.life.storage.caches.RangedCache.Rangable.Companion.startAndEndInMs
import com.myxoz.life.storage.convertion.SyncableFromEntity.fromEntity
import com.myxoz.life.storage.dbwrapper.commits.ReadCommitsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class CommitsInterface internal constructor(
    private val readCommitsDao: ReadCommitsDao,
    appScope: CoroutineScope
) {
    private val zone = ZoneId.systemDefault()
    private val _repos = PerformantCache<String, CommitSyncable>(appScope) {
        error("Why tf was this called? If this errors you really did a lot wrong. Fetching a repo by key eventhough we dont even know the Repo, props. Ah and btw, you attempted to fetch $it.")
    }
    val getAllRepos = _repos.allValuesFlow.map{ it.sortedByDescending { repo -> repo.commitDate ?: 0L } }

    private val _commits = InterlockedCache.dayedSame(
        appScope,
        { first, other -> first.commitSha == other.commitSha },
        {
            listOfNotNull(it.commitDate?.toLocalDate(zone))
        },
        { it.commitSha },
        { key  ->
            readCommitsDao.getBySha(key)?.fromEntity()
                ?: error("Fetched commit by non existent sha. Where did you get this from? $key")
        },
        {range ->
            val (start, end) = range.startAndEndInMs(zone)
            readCommitsDao.getCommitsForDay(start, end)
                .map { entity -> entity.fromEntity() }
        }
    ) { _, new ->
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

    fun getAllCommitsFor(repoName: String) = _commits.allValuesFlow.map { commit -> commit.filter { it.repoName == repoName }.sortedByDescending { it.commitDate } }
    internal fun updateCommit(commitSyncable: CommitSyncable) {
        _commits.overwrite(commitSyncable.commitSha, commitSyncable)
    }

    // val entity = db.getBySha(sha) ?: throw Error("Tried to get unknown commit. This is usupported. Check where you have the sha from.")
    // return from(entity)

    fun getCommit(sha: String) = _commits.flowFor(sha)
    init {
        appScope.launch {
            _commits.markAllEntriesAsLoaded()
            _commits.overwriteAll(
                readCommitsDao.getAllCommitsEver().map {
                    it.commitSha to it.fromEntity()
                }
            )
        }
    }
}