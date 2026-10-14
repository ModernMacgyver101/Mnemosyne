package com.hardlinecoding.checkthebox.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hardlinecoding.checkthebox.model.LoggedEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggedEventDao {
    @Query("SELECT * FROM logged_events ORDER BY eventTimestamp DESC")
    fun getAllEvents(): Flow<List<LoggedEvent>>

    @Insert
    suspend fun insertEvent(event: LoggedEvent)

    @Query("DELETE FROM logged_events")
    suspend fun clearAllEvents()
}
