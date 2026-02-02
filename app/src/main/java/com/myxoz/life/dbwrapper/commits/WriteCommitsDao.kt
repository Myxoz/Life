package com.myxoz.life.dbwrapper.commits

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface WriteCommitsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(commit: CommitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commits: List<CommitEntity>)
}