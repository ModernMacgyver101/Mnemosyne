package com.hardlinecoding.checkthebox.data

import androidx.room.*
import com.hardlinecoding.checkthebox.model.ToDoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoItemDao {
    @Query("SELECT * FROM todo_items ORDER BY isPinned DESC, id DESC")
    fun getAllToDoItems(): Flow<List<ToDoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToDoItem(item: ToDoItem)

    @Update
    suspend fun updateToDoItem(item: ToDoItem)

    @Delete
    suspend fun deleteToDoItem(item: ToDoItem)

    @Query("DELETE FROM todo_items")
    suspend fun clearAllToDoItems()
}
