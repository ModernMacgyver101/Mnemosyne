package com.hardlinecoding.checkthebox.data

import androidx.room.*
import com.hardlinecoding.checkthebox.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY modifiedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
