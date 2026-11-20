package com.myxoz.life.storage.api.syncables

import com.myxoz.life.storage.api.helper.CommitModification
import java.time.Instant

data class CommitSyncable(
    val repoOwner: String,
    val repoName: String,
    val commitSha: String,
    val commitMessage: String?,
    val commitAuthor: String?,
    val commitEmail: String?,
    val commitDate: Instant?,
    val additions: Int?,
    val deletions: Int?,
    val filesChanged: Int?,
    val modifications: List<CommitModification>,
    val commitUrl: String?,
    val updated: Instant
) : ServerSyncable {
    internal fun toRepoKey() = "$repoOwner/$repoName"
}