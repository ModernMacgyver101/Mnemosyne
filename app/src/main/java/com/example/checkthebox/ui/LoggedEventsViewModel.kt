package com.hardlinecoding.checkthebox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hardlinecoding.checkthebox.data.AppDatabase
import com.hardlinecoding.checkthebox.model.EventType
import com.hardlinecoding.checkthebox.model.LoggedEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class LoggedEventsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).loggedEventDao()

    val loggedEvents: StateFlow<List<LoggedEvent>> = dao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logEvent(title: String, description: String, type: EventType) {
        viewModelScope.launch {
            dao.insertEvent(
                LoggedEvent(
                    eventTimestamp = LocalDateTime.now(),
                    eventType = type,
                    title = title,
                    description = description
                )
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearAllEvents()
        }
    }
}
