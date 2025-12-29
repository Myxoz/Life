package com.myxoz.life.integration

import com.myxoz.life.api.CommitSyncable

object GitHub {
    fun generateLinkForFileOfCommit(
        commit: CommitSyncable,
        modification: CommitSyncable.Companion.CommitModification
    ) = "${commit.commitUrl?.replace("commit", "blob")}/${modification.filename}"
    fun generateLinkForRepo(commit: CommitSyncable) = "https://github.com/${commit.repoOwner}/${commit.repoName}"
}