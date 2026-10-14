package com.hardlinecoding.checkthebox.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class EventType {
    TASK_COMPLETED,
    REMINDER_TRIGGERED,
    TASK_DELETED,
    REMINDER_DELETED
}

@Entity(tableName = "logged_events")
data class LoggedEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventTimestamp: LocalDateTime,
    val eventType: EventType,
    val title: String,
    val description: String
)
