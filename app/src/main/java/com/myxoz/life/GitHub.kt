package com.myxoz.life

import com.myxoz.life.storage.api.helper.CommitModification
import com.myxoz.life.storage.api.syncables.CommitSyncable

object GitHub {
    fun generateLinkForFileOfCommit(
        commit: CommitSyncable,
        modification: CommitModification
    ) = "${commit.commitUrl?.replace("commit", "blob")}/${modification.filename}"
    fun generateLinkForRepo(commit: CommitSyncable) = "https://github.com/${commit.repoOwner}/${commit.repoName}"
}