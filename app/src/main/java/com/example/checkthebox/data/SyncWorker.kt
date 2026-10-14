package com.hardlinecoding.checkthebox.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val email = prefs.getString("user_email", null) ?: return Result.failure()
        
        val drive = GoogleDriveSyncService.createDriveService(applicationContext, email)
        val syncService = GoogleDriveSyncService(drive)

        return try {
            // Use the static close method to ensure the instance is cleared
            AppDatabase.closeDatabase()
            
            val dbFile = applicationContext.getDatabasePath("mnemosyne_database")
            if (dbFile.exists()) {
                syncService.uploadFile(dbFile, "backup_mnemosyne.db")
                Log.d("SyncWorker", "Auto backup successful")
                
                // Re-open and signal refresh so UI stays connected
                AppDatabase.getDatabase(applicationContext)
                AppDatabase.triggerRefresh()
                
                // Update last sync time in SharedPreferences
                val currentTime = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
                prefs.edit().putString("last_sync_time", "Sync successful: $currentTime").apply()
                
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Auto backup failed", e)
            Result.retry()
        }
    }
}