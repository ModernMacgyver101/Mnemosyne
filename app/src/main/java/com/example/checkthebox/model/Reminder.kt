package com.hardlinecoding.checkthebox.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

enum class Recurrence {
    NONE, DAILY, WEEKLY, EVERY_2_WEEKS, MONTHLY, EVERY_2_MONTHS, YEARLY
}

enum class ReminderColor(val label: String, val light: Long, val dark: Long) {
    RED("Red", 0xFFFFCDD2, 0xFFE57373),
    BLUE("Blue", 0xFFBBDEFB, 0xFF64B5F6),
    YELLOW("Yellow", 0xFFFFF9C4, 0xFFFFF176),
    GREEN("Green", 0xFFC8E6C9, 0xFF81C784),
    PURPLE("Purple", 0xFFE1BEE7, 0xFFBA68C8),
    ORANGE("Orange", 0xFFFFE0B2, 0xFFFFB74D),
    GREY("Grey", 0xFFF5F5F5, 0xFFE0E0E0);

    companion object {
        fun fromName(name: String?): ReminderColor = entries.find { it.name == name } ?: GREY
    }
}

data class ReminderContact(
    val name: String,
    val phoneNumber: String
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val dateTime: LocalDateTime,
    val isNotificationEnabled: Boolean,
    val isSmsEnabled: Boolean = false,
    val contacts: List<ReminderContact> = emptyList(),
    val recurrence: Recurrence,
    val color: ReminderColor = ReminderColor.GREY
)
