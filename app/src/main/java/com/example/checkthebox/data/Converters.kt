package com.hardlinecoding.checkthebox.data

import androidx.room.TypeConverter
import com.hardlinecoding.checkthebox.model.Priority
import com.hardlinecoding.checkthebox.model.Recurrence
import com.hardlinecoding.checkthebox.model.ReminderColor
import com.hardlinecoding.checkthebox.model.ReminderContact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun fromRecurrence(value: String): Recurrence {
        return Recurrence.valueOf(value)
    }

    @TypeConverter
    fun recurrenceToString(recurrence: Recurrence): String {
        return recurrence.name
    }

    @TypeConverter
    fun fromReminderColor(value: String): ReminderColor {
        return ReminderColor.valueOf(value)
    }

    @TypeConverter
    fun reminderColorToString(color: ReminderColor): String {
        return color.name
    }

    @TypeConverter
    fun fromContactList(value: String): List<ReminderContact> {
        val listType = object : TypeToken<List<ReminderContact>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun contactListToString(list: List<ReminderContact>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun fromPriority(value: String): Priority {
        return try { Priority.valueOf(value) } catch (e: Exception) { Priority.ROUTINE }
    }

    @TypeConverter
    fun priorityToString(priority: Priority): String {
        return priority.name
    }
}
