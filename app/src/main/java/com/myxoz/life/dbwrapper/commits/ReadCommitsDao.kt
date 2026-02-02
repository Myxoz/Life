package com.myxoz.life.dbwrapper.commits

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReadCommitsDao {
    @Query("SELECT * FROM commits WHERE repo_owner = :owner AND repo_name = :repo ORDER BY commit_date DESC")
    suspend fun getCommitsForRepo(owner: String, repo: String): List<CommitEntity>

    @Query("SELECT * FROM commits WHERE commit_date > :start AND commit_date <= :ends ORDER BY commit_date DESC")
    suspend fun getCommitsForDay(start: Long, ends: Long): List<CommitEntity>

    @Query("SELECT * FROM commits")
    suspend fun getAllCommitsEver(): List<CommitEntity>

    @Query("SELECT * FROM commits WHERE repo_name = :repoName")
    suspend fun getAllForRepo(repoName: String): List<CommitEntity>

    @Query("SELECT * FROM commits WHERE commit_sha = :sha LIMIT 1")
    suspend fun getBySha(sha: String): CommitEntity?
}