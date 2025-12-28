package com.myxoz.life.dbwrapper

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

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

@Dao
interface CommitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commit: CommitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commits: List<CommitEntity>)

    @Query("SELECT * FROM commits WHERE repo_owner = :owner AND repo_name = :repo ORDER BY commit_date DESC")
    suspend fun getCommitsForRepo(owner: String, repo: String): List<CommitEntity>

    @Query("SELECT * FROM commits WHERE commit_date > :start AND commit_date <= :ends ORDER BY commit_date DESC")
    suspend fun getCommitsForDay(start: Long, ends: Long): List<CommitEntity>

    @Query("SELECT * FROM commits WHERE commit_sha = :sha LIMIT 1")
    suspend fun getBySha(sha: String): CommitEntity?
}