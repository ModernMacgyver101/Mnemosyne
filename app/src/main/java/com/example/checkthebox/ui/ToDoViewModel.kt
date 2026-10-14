package com.hardlinecoding.checkthebox.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hardlinecoding.checkthebox.data.AppDatabase
import com.hardlinecoding.checkthebox.model.ToDoItem
import com.hardlinecoding.checkthebox.model.EventType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.hardlinecoding.checkthebox.model.LoggedEvent

class ToDoViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).toDoItemDao()
    private val eventDao = AppDatabase.getDatabase(application).loggedEventDao()

    val toDoItems: StateFlow<List<ToDoItem>> = dao.getAllToDoItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToDoItem(item: ToDoItem) {
        viewModelScope.launch {
            dao.insertToDoItem(item)
        }
    }

    fun updateToDoItem(item: ToDoItem) {
        viewModelScope.launch {
            val oldItem = toDoItems.value.find { it.id == item.id }
            
            if (item.isCompleted && oldItem?.isCompleted == false) {
                eventDao.insertEvent(
                    LoggedEvent(
                        eventTimestamp = LocalDateTime.now(),
                        eventType = EventType.TASK_COMPLETED,
                        title = item.title,
                        description = item.description
                    )
                )
                dao.deleteToDoItem(item)
            } else {
                dao.updateToDoItem(item)
            }
        }
    }

    fun deleteToDoItem(item: ToDoItem) {
        viewModelScope.launch {
            dao.deleteToDoItem(item)
            eventDao.insertEvent(
                LoggedEvent(
                    eventTimestamp = LocalDateTime.now(),
                    eventType = EventType.TASK_DELETED,
                    title = item.title,
                    description = item.description
                )
            )
        }
    }

    fun clearAllToDoItems() {
        viewModelScope.launch {
            dao.clearAllToDoItems()
        }
    }
}
