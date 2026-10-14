package com.hardlinecoding.checkthebox.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.hardlinecoding.checkthebox.data.AppDatabase
import com.hardlinecoding.checkthebox.model.EventType
import com.hardlinecoding.checkthebox.model.LoggedEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val isHiddenModeEnabled = prefs.getBoolean("hidden_mode_enabled", false)
        
        if (isHiddenModeEnabled) {
            Log.d("ReminderReceiver", "Notification suppressed due to Hidden Mode")
            return
        }

        val type = intent.getStringExtra("type") ?: "NOTIFICATION"
        val title = intent.getStringExtra("title") ?: "Reminder"
        val description = intent.getStringExtra("description") ?: ""
        val formattedDate = intent.getStringExtra("formattedDate") ?: ""
        val shortDescription = intent.getStringExtra("shortDescription") ?: ""
        val notificationId = intent.getLongExtra("id", 0).toInt()
        val phoneNumber = intent.getStringExtra("phoneNumber")

        when {
            type == "NOTIFICATION" -> {
                showNotification(context, title, formattedDate, shortDescription, notificationId)
                logTriggeredEvent(context, title, description)
            }
            type.startsWith("SMS_") -> {
                sendSms(context, title, description, formattedDate, phoneNumber)
            }
        }
    }

    private fun logTriggeredEvent(context: Context, title: String, description: String) {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.getDatabase(context).loggedEventDao().insertEvent(
                LoggedEvent(
                    eventTimestamp = LocalDateTime.now(),
                    eventType = EventType.REMINDER_TRIGGERED,
                    title = title,
                    description = description
                )
            )
        }
    }

    private fun showNotification(context: Context, title: String, formattedDate: String, shortDescription: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val contentText = if (shortDescription.isNotBlank()) {
            "Due: $formattedDate\n$shortDescription"
        } else {
            "Due: $formattedDate"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Due: $formattedDate")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun sendSms(context: Context, title: String, description: String, formattedDate: String, phoneNumber: String?) {
        if (phoneNumber.isNullOrBlank()) return

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            val message = "Reminder: $title\nDescription: $description\nDue: $formattedDate"
            
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.d("ReminderReceiver", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Failed to send SMS", e)
        }
    }
}
