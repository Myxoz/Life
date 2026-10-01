package com.myxoz.life.storage.api.helper

data class CommitModification(
    val filename: String,
    val additions: Int,
    val deletions: Int,
    val changes: Int,
    val status: String,
)
