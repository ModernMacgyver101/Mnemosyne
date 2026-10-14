package com.hardlinecoding.checkthebox.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hardlinecoding.checkthebox.model.Reminder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class NotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        // Schedule standard notification
        if (reminder.isNotificationEnabled) {
            scheduleNotification(reminder)
        }

        // Schedule SMS reminders if enabled
        if (reminder.isSmsEnabled && reminder.contacts.isNotEmpty()) {
            scheduleSmsReminders(reminder)
        }
    }

    private fun scheduleNotification(reminder: Reminder) {
        val intent = createIntent(reminder, "NOTIFICATION")
        val pendingIntent = createPendingIntent(reminder.id.toInt(), intent)
        val triggerAtMillis = reminder.dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        setAlarm(triggerAtMillis, pendingIntent)
    }

    private fun scheduleSmsReminders(reminder: Reminder) {
        val now = LocalDateTime.now()
        val scheduledTime = reminder.dateTime

        val smsIntervals = listOf(
            scheduledTime.minusDays(3) to "SMS_3_DAYS",
            scheduledTime.minusDays(1) to "SMS_1_DAY",
            scheduledTime.minusHours(1) to "SMS_1_HOUR",
            scheduledTime to "SMS_SCHEDULED"
        )

        smsIntervals.forEach { (triggerTime, tag) ->
            if (triggerTime.isAfter(now)) {
                reminder.contacts.forEach { contact ->
                    val intent = createIntentForContact(reminder, tag, contact.phoneNumber)
                    val requestCode = reminder.id.toInt() + tag.hashCode() + contact.phoneNumber.hashCode()
                    val pendingIntent = createPendingIntent(requestCode, intent)
                    val triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    setAlarm(triggerAtMillis, pendingIntent)
                }
            }
        }
    }

    private fun createIntent(reminder: Reminder, type: String): Intent {
        val formattedDate = reminder.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
        val descriptionWords = reminder.description.trim().split("\\s+".toRegex())
        val shortDescription = if (descriptionWords.size > 6) {
            descriptionWords.take(6).joinToString(" ") + "..."
        } else {
            reminder.description
        }

        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra("type", type)
            putExtra("title", reminder.title)
            putExtra("description", reminder.description)
            putExtra("formattedDate", formattedDate)
            putExtra("shortDescription", shortDescription)
            putExtra("id", reminder.id)
        }
    }

    private fun createIntentForContact(reminder: Reminder, type: String, phoneNumber: String): Intent {
        val intent = createIntent(reminder, type)
        intent.putExtra("phoneNumber", phoneNumber)
        return intent
    }

    private fun createPendingIntent(requestCode: Int, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(reminder: Reminder) {
        // Cancel Notification
        cancelAlarm(reminder.id.toInt())

        // Cancel SMS Alarms
        val smsTags = listOf("SMS_3_DAYS", "SMS_1_DAY", "SMS_1_HOUR", "SMS_SCHEDULED")
        smsTags.forEach { tag ->
            reminder.contacts.forEach { contact ->
                val requestCode = reminder.id.toInt() + tag.hashCode() + contact.phoneNumber.hashCode()
                cancelAlarm(requestCode)
            }
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
