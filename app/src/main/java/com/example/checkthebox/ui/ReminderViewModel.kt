package com.hardlinecoding.checkthebox.ui

import android.app.Application
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hardlinecoding.checkthebox.data.AppDatabase
import com.hardlinecoding.checkthebox.model.Reminder
import com.hardlinecoding.checkthebox.model.EventType
import com.hardlinecoding.checkthebox.notification.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.hardlinecoding.checkthebox.model.LoggedEvent

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val reminders: StateFlow<List<Reminder>> = AppDatabase.refreshSignal
        .flatMapLatest { 
            AppDatabase.getDatabase(getApplication()).reminderDao().getAllReminders() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _appTheme = MutableStateFlow(AppTheme.AUTO)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _colorTheme = MutableStateFlow(ColorTheme.DEFAULT)
    val colorTheme: StateFlow<ColorTheme> = _colorTheme.asStateFlow()

    private val _isHiddenModeEnabled = MutableStateFlow(false)
    val isHiddenModeEnabled: StateFlow<Boolean> = _isHiddenModeEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Encrypted Shared Preferences
    private val masterKey = MasterKey.Builder(application)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        application,
        "secure_sync_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val scheduler = NotificationScheduler(application)
    private val eventDao = AppDatabase.getDatabase(application).loggedEventDao()

    init {
        val savedTheme = prefs.getString("app_theme", AppTheme.AUTO.name)
        _appTheme.value = try { AppTheme.valueOf(savedTheme ?: AppTheme.AUTO.name) } catch (e: Exception) { AppTheme.AUTO }
        
        val savedColorTheme = prefs.getString("color_theme", ColorTheme.DEFAULT.name)
        _colorTheme.value = try { ColorTheme.valueOf(savedColorTheme ?: ColorTheme.DEFAULT.name) } catch (e: Exception) { ColorTheme.DEFAULT }

        _isHiddenModeEnabled.value = prefs.getBoolean("hidden_mode_enabled", false)
        if (_isHiddenModeEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication()).reminderDao().insertReminder(reminder)
            scheduler.schedule(reminder)
        }
    }

    fun updateReminder(updatedReminder: Reminder) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication()).reminderDao().updateReminder(updatedReminder)
            scheduler.cancel(updatedReminder)
            scheduler.schedule(updatedReminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication()).reminderDao().deleteReminder(reminder)
            scheduler.cancel(reminder)
            eventDao.insertEvent(
                LoggedEvent(
                    eventTimestamp = LocalDateTime.now(),
                    eventType = EventType.REMINDER_DELETED,
                    title = reminder.title,
                    description = reminder.description
                )
            )
        }
    }

    fun deleteReminders(remindersToDelete: List<Reminder>) {
        viewModelScope.launch {
            remindersToDelete.forEach { reminder ->
                AppDatabase.getDatabase(getApplication()).reminderDao().deleteReminder(reminder)
                scheduler.cancel(reminder)
                eventDao.insertEvent(
                    LoggedEvent(
                        eventTimestamp = LocalDateTime.now(),
                        eventType = EventType.REMINDER_DELETED,
                        title = reminder.title,
                        description = reminder.description
                    )
                )
            }
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            reminders.value.forEach { scheduler.cancel(it) }
            AppDatabase.getDatabase(getApplication()).reminderDao().clearAllReminders()
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

    fun setHiddenModeEnabled(enabled: Boolean, context: Context) {
        _isHiddenModeEnabled.value = enabled
        prefs.edit().putBoolean("hidden_mode_enabled", enabled).apply()
        
        // Update app icon/name
        updateAppIcon(enabled, context)
        
        if (enabled) {
            _isAppUnlocked.value = false
        } else {
            _isAppUnlocked.value = true
        }
    }

    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    fun getSavedPin(): String? {
        return prefs.getString("hidden_mode_pin", null)
    }

    fun savePin(pin: String) {
        prefs.edit().putString("hidden_mode_pin", pin).apply()
    }

    private fun updateAppIcon(hiddenMode: Boolean, context: Context) {
        val packageManager = context.packageManager
        
        val defaultLauncher = ComponentName(context, "com.hardlinecoding.checkthebox.LauncherDefault")
        val calculatorLauncher = ComponentName(context, "com.hardlinecoding.checkthebox.LauncherCalculator")

        if (hiddenMode) {
            packageManager.setComponentEnabledSetting(
                defaultLauncher,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                calculatorLauncher,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            packageManager.setComponentEnabledSetting(
                calculatorLauncher,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            packageManager.setComponentEnabledSetting(
                defaultLauncher,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
