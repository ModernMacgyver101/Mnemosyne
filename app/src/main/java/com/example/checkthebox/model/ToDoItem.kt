package com.hardlinecoding.checkthebox.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority(val label: String, val level: Int) {
    IMMEDIATE("Immediate", 0),
    URGENT("Urgent", 1),
    PRIORITY("Priority", 2),
    ROUTINE("Routine", 3),
    CONVENIENCE("Convenience", 4)
}

@Entity(tableName = "todo_items")
data class ToDoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val priority: Priority = Priority.ROUTINE
)
