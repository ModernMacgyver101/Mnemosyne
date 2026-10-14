package com.hardlinecoding.checkthebox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hardlinecoding.checkthebox.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = NotificationScheduler(context)
            val db = AppDatabase.getDatabase(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val reminders = db.reminderDao().getAllRemindersList()
                reminders.forEach { reminder ->
                    scheduler.schedule(reminder)
                }
            }
        }
    }
}
