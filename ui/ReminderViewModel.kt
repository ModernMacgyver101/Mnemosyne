package com.example.checkthebox.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.checkthebox.data.AppDatabase
import com.example.checkthebox.data.GoogleDriveSyncService
import com.example.checkthebox.model.Reminder
import com.example.checkthebox.notification.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    
    private val _refreshTrigger = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val reminders: StateFlow<List<Reminder>> = _refreshTrigger
        .flatMapLatest { database.reminderDao().getAllReminders() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appTheme = MutableStateFlow(AppTheme.AUTO)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _colorTheme = MutableStateFlow(ColorTheme.DEFAULT)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val prefs = application.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    init {
        _lastSyncTime.value = prefs.getString("last_sync_time", "Never synced")
        _userEmail.value = prefs.getString("user_email", null)
        
        val savedTheme = prefs.getString("app_theme", AppTheme.AUTO.name)
        _appTheme.value = try { AppTheme.valueOf(savedTheme ?: AppTheme.AUTO.name) } catch (e: Exception) { AppTheme.AUTO }
        
        val savedColorTheme = prefs.getString("color_theme", ColorTheme.DEFAULT.name)
        _colorTheme.value = try { ColorTheme.valueOf(savedColorTheme ?: ColorTheme.DEFAULT.name) } catch (e: Exception) { ColorTheme.DEFAULT }
    }

    private val scheduler = NotificationScheduler(application)
    private var syncService: GoogleDriveSyncService? = null

    fun setupSync(email: String) {
        _userEmail.value = email
        prefs.edit().putString("user_email", email).apply()
        val drive = GoogleDriveSyncService.createDriveService(getApplication(), email)
        syncService = GoogleDriveSyncService(drive)
        
        performInitialSync()
    }

    fun signOut() {
        _userEmail.value = null
        prefs.edit().remove("user_email").apply()
        syncService = null
    }

    fun syncWithDrive() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                uploadDatabase()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private suspend fun updateSyncStatus(success: Boolean) {
        val currentTime = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val statusText = if (success) "Sync successful: $currentTime" else "Sync failed: $currentTime"
        withContext(Dispatchers.Main) {
            _lastSyncTime.value = statusText
        }
        prefs.edit().putString("last_sync_time", statusText).apply()
    }

    private suspend fun uploadDatabase() {
        val service = syncService ?: return
        try {
            Log.d("Sync", "Attempting to upload database to Drive...")
            val dbFile = getApplication<Application>().getDatabasePath("mnemosyne_database")
            if (dbFile.exists()) {
                AppDatabase.closeDatabase()
                service.syncDataToDrive(dbFile, "backup_mnemosyne.db")
                AppDatabase.getDatabase(getApplication())
                updateSyncStatus(true)
                Log.d("Sync", "Database upload successful.")
            } else {
                Log.w("Sync", "Database file does not exist, cannot upload.")
                updateSyncStatus(false)
            }
        } catch (e: Exception) {
            Log.e("Sync", "Database upload failed.", e)
            updateSyncStatus(false)
        }
    }

    fun performInitialSync() {
        val service = syncService ?: return
        if (_isSyncing.value) return
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                Log.d("Sync", "Checking for existing backup file...")
                val dbFile = getApplication<Application>().getDatabasePath("mnemosyne_database")
                val tempFile = File(getApplication<Application>().cacheDir, "temp_backup.db")
                
                val cloudTimestamp = FileOutputStream(tempFile).use { fos ->
                    service.downloadDataFromDrive("backup_mnemosyne.db", fos)
                }
                
                if (cloudTimestamp != null && tempFile.exists() && tempFile.length() > 0) {
                    Log.d("Sync", "Backup file found, restoring from Drive. Size: ${tempFile.length()}")
                    
                    AppDatabase.closeDatabase()
                    
                    // Delete WAL and SHM files to prevent corruption after restore
                    File(dbFile.path + "-wal").delete()
                    File(dbFile.path + "-shm").delete()
                    
                    tempFile.copyTo(dbFile, overwrite = true)
                    
                    // Crucial: Set the local file timestamp to match the cloud exactly
                    dbFile.setLastModified(cloudTimestamp)
                    
                    _refreshTrigger.value += 1
                    updateSyncStatus(true)
                    Log.d("Sync", "Restore successful.")
                } else {
                    Log.d("Sync", "No backup file found or download failed. Performing initial backup.")
                    uploadDatabase()
                }
                tempFile.delete()
            } catch (e: Exception) {
                Log.e("Sync", "Initial sync/restore failed.", e)
                updateSyncStatus(false)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            database.reminderDao().insertReminder(reminder)
            scheduler.schedule(reminder)
            syncWithDrive()
        }
    }

    fun updateReminder(updatedReminder: Reminder) {
        viewModelScope.launch {
            database.reminderDao().updateReminder(updatedReminder)
            scheduler.cancel(updatedReminder)
            scheduler.schedule(updatedReminder)
            syncWithDrive()
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            database.reminderDao().deleteReminder(reminder)
            scheduler.cancel(reminder)
            syncWithDrive()
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            reminders.value.forEach { scheduler.cancel(it) }
            database.reminderDao().clearAllReminders()
            syncWithDrive()
        }
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun setColorTheme(theme: ColorTheme) {
        _colorTheme.value = theme
        prefs.edit().putString("color_theme", theme.name).apply()
    }
}
