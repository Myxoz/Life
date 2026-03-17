package com.myxoz.life.dbwrapper.todos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WriteTodosDao {
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun removeTodoById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)
}