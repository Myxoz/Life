package com.myxoz.life.dbwrapper.commits

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commits")
data class CommitEntity(
    @ColumnInfo(name = "repo_owner")
    val repoOwner: String,

    @ColumnInfo(name = "repo_name")
    val repoName: String,

    @PrimaryKey
    @ColumnInfo(name = "commit_sha")
    val commitSha: String,

    @ColumnInfo(name = "commit_message")
    val commitMessage: String?,

    @ColumnInfo(name = "commit_author")
    val commitAuthor: String?,

    @ColumnInfo(name = "commit_email")
    val commitEmail: String?,

    @ColumnInfo(name = "commit_date")
    val commitDate: Long?,

    @ColumnInfo(name = "additions")
    val additions: Int?,

    @ColumnInfo(name = "deletions")
    val deletions: Int?,

    @ColumnInfo(name = "files_changed")
    val filesChanged: Int?,

    @ColumnInfo(name = "files_json")
    val filesJson: String?,

    @ColumnInfo(name = "commit_url")
    val commitUrl: String?,

    @ColumnInfo(name = "updated")
    val updated: Long
)