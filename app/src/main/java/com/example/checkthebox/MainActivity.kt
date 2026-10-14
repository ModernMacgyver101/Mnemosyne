package com.hardlinecoding.checkthebox

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hardlinecoding.checkthebox.notification.ToDoReminderWorker
import com.hardlinecoding.checkthebox.ui.*
import com.hardlinecoding.checkthebox.ui.theme.MnemosyneTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class Screen(val title: String, val icon: ImageVector) {
    ToDo("To Do List", Icons.AutoMirrored.Filled.List),
    Reminders("Reminder", Icons.Default.Notifications),
    Notes("Notes", Icons.Default.Description),
    LoggedEvents("Archive", Icons.Default.History)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ReminderViewModel by viewModels()
    private val toDoViewModel: ToDoViewModel by viewModels()
    private val loggedEventsViewModel: LoggedEventsViewModel by viewModels()
    private val noteViewModel: NoteViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No action needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askNotificationPermission()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val colorTheme by viewModel.colorTheme.collectAsState()
            val isHiddenModeEnabled by viewModel.isHiddenModeEnabled.collectAsState()
            val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
            
            val isDarkTheme = when (appTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.AUTO -> isSystemInDarkTheme()
            }

            MnemosyneTheme(darkTheme = isDarkTheme, colorTheme = colorTheme) {
                if (isHiddenModeEnabled && !isAppUnlocked) {
                    CalculatorDecoy(
                        onUnlock = { viewModel.setAppUnlocked(true) },
                        correctPin = viewModel.getSavedPin()
                    )
                } else {
                    val pagerState = rememberPagerState(initialPage = Screen.Reminders.ordinal) { Screen.entries.size }
                    val coroutineScope = rememberCoroutineScope()

                    var showAddReminderDialog by remember { mutableStateOf(false) }
                    var showAddToDoDialog by remember { mutableStateOf(false) }
                    var showAddNoteDialog by remember { mutableStateOf(false) }
                    var showSettingsDialog by remember { mutableStateOf(false) }
                    var showClearRemindersConfirm by remember { mutableStateOf(false) }
                    var showClearToDoConfirm by remember { mutableStateOf(false) }
                    var showClearArchiveConfirm by remember { mutableStateOf(false) }
                    var isEditingNote by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (showSettingsDialog) {
                            SettingsDialog(
                                viewModel = viewModel,
                                onDismiss = { showSettingsDialog = false }
                            )
                        }
                        
                        if (showClearArchiveConfirm) {
                            AlertDialog(
                                onDismissRequest = { showClearArchiveConfirm = false },
                                title = { Text("Clear Archive") },
                                text = { Text("Are you sure you want to delete all logged events? This action cannot be undone.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            loggedEventsViewModel.clearLogs()
                                            showClearArchiveConfirm = false
                                        }
                                    ) {
                                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearArchiveConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        Scaffold(
                            topBar = {
                                if (!isEditingNote) {
                                    Column {
                                        Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
                                        TabRow(selectedTabIndex = pagerState.currentPage) {
                                            Screen.entries.forEachIndexed { index, screen ->
                                                Tab(
                                                    selected = pagerState.currentPage == index,
                                                    onClick = { 
                                                        coroutineScope.launch {
                                                            pagerState.animateScrollToPage(index)
                                                        }
                                                    },
                                                    text = { Text(screen.title) },
                                                    icon = { Icon(screen.icon, contentDescription = null) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            floatingActionButton = {
                                if (!isEditingNote) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        SmallFloatingActionButton(
                                            onClick = { showSettingsDialog = true },
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                                        }
                                        
                                        val currentScreen = Screen.entries[pagerState.currentPage]
                                        val isArchivePage = currentScreen == Screen.LoggedEvents
                                        
                                        FloatingActionButton(
                                            onClick = { 
                                                when (currentScreen) {
                                                    Screen.Reminders -> showAddReminderDialog = true
                                                    Screen.ToDo -> showAddToDoDialog = true
                                                    Screen.Notes -> showAddNoteDialog = true
                                                    Screen.LoggedEvents -> showClearArchiveConfirm = true
                                                }
                                            },
                                            containerColor = if (isArchivePage) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = if (isArchivePage) Color.Red else MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Icon(
                                                imageVector = if (isArchivePage) Icons.Default.Clear else Icons.Default.Add,
                                                contentDescription = if (isArchivePage) "Clear Archive" else "Add"
                                            )
                                        }
                                    }
                                }
                            },
                            floatingActionButtonPosition = FabPosition.End
                        ) { innerPadding ->
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (isEditingNote) PaddingValues(0.dp) else innerPadding)
                            ) { page ->
                                when (Screen.entries[page]) {
                                    Screen.ToDo -> ToDoScreen(
                                        reminderViewModel = viewModel, 
                                        viewModel = toDoViewModel,
                                        showAddDialog = showAddToDoDialog,
                                        onAddDialogDismiss = { showAddToDoDialog = false },
                                        showSettingsDialog = false, 
                                        onSettingsDialogDismiss = {},
                                        showClearConfirm = showClearToDoConfirm,
                                        onClearConfirmDismiss = { showClearToDoConfirm = false }
                                    )
                                    Screen.Reminders -> ReminderScreen(
                                        viewModel = viewModel,
                                        showAddDialog = showAddReminderDialog,
                                        onAddDialogDismiss = { showAddReminderDialog = false },
                                        showSettingsDialog = false, 
                                        onSettingsDialogDismiss = {},
                                        showClearConfirm = showClearRemindersConfirm,
                                        onClearConfirmDismiss = { showClearRemindersConfirm = false }
                                    )
                                    Screen.Notes -> NoteScreen(
                                        viewModel = noteViewModel,
                                        showAddDialog = showAddNoteDialog,
                                        onAddDialogDismiss = { showAddNoteDialog = false },
                                        onEditingStateChange = { isEditingNote = it }
                                    )
                                    Screen.LoggedEvents -> LoggedEventsScreen(viewModel = loggedEventsViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (toDoViewModel.toDoItems.value.any { !it.isCompleted }) scheduleToDoReminder()
    }

    override fun onStart() {
        super.onStart()
        WorkManager.getInstance(this).cancelUniqueWork("todo_reminder_work")
    }

    private fun scheduleToDoReminder() {
        val workRequest = OneTimeWorkRequestBuilder<ToDoReminderWorker>()
            .setInitialDelay(6, TimeUnit.HOURS)
            .addTag("todo_reminder")
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork("todo_reminder_work", ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun askNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
