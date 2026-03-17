package com.myxoz.life.dbwrapper.todos

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ReadTodosDao {
    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TodoEntity?


    @Query("SELECT * FROM todos WHERE timestamp > :start AND timestamp <= :ends ORDER BY timestamp ASC")
    suspend fun getTodosBetween(start: Long, ends: Long): List<TodoEntity>
}