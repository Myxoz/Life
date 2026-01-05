package com.myxoz.life.android.integration

import com.myxoz.life.api.syncables.CommitSyncable

object GitHub {
    fun generateLinkForFileOfCommit(
        commit: CommitSyncable,
        modification: CommitSyncable.Companion.CommitModification
    ) = "${commit.commitUrl?.replace("commit", "blob")}/${modification.filename}"
    fun generateLinkForRepo(commit: CommitSyncable) = "https://github.com/${commit.repoOwner}/${commit.repoName}"
}