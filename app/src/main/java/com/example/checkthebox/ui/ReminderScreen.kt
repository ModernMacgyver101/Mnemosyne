package com.hardlinecoding.checkthebox.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hardlinecoding.checkthebox.model.Recurrence
import com.hardlinecoding.checkthebox.model.Reminder
import com.hardlinecoding.checkthebox.model.ReminderColor
import com.hardlinecoding.checkthebox.model.ReminderContact
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderScreen(
    viewModel: ReminderViewModel = viewModel(),
    showAddDialog: Boolean,
    onAddDialogDismiss: () -> Unit,
    showSettingsDialog: Boolean,
    onSettingsDialogDismiss: () -> Unit,
    showClearConfirm: Boolean,
    onClearConfirmDismiss: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()
    
    var viewingReminder by remember { mutableStateOf<Reminder?>(null) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
    
    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedReminders = remember { mutableStateListOf<Reminder>() }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedReminders.clear()
    }

    BackHandler(enabled = isSelectionMode) {
        exitSelectionMode()
    }

    // Grouping reminders
    val groupedReminders = remember(reminders) {
        val now = LocalDateTime.now()
        reminders.groupBy { reminder ->
            val days = ChronoUnit.DAYS.between(now.toLocalDate(), reminder.dateTime.toLocalDate())
            when {
                days < 0 -> "Past Due"
                days <= 7 -> "1 - 7 Days"
                days <= 30 -> "8 - 30 Days"
                days <= 60 -> "31 - 60 Days"
                days <= 90 -> "61 - 90 Days"
                else -> "90 Days and Beyond"
            }
        }.toSortedMap(compareBy { label ->
            when (label) {
                "Past Due" -> -1
                "1 - 7 Days" -> 0
                "8 - 30 Days" -> 1
                "31 - 60 Days" -> 2
                "61 - 90 Days" -> 3
                else -> 4
            }
        })
    }
    
    LaunchedEffect(groupedReminders) {
        groupedReminders.keys.forEach { label ->
            if (!expandedStates.containsKey(label)) {
                expandedStates[label] = true
            }
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TopAppBar(
                    title = { Text("${selectedReminders.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedReminders.clear()
                            selectedReminders.addAll(reminders)
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            viewModel.deleteReminders(selectedReminders.toList())
                            exitSelectionMode()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                groupedReminders.forEach { (label, groupReminders) ->
                    val isExpanded = expandedStates[label] ?: true
                    
                    stickyHeader {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedStates[label] = !isExpanded },
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (label == "Past Due") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    if (isExpanded) {
                        items(groupReminders, key = { it.id }) { reminder ->
                            val isSelected = selectedReminders.contains(reminder)
                            SwipeToDeleteReminder(
                                reminder = reminder,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedReminders.remove(reminder)
                                        else selectedReminders.add(reminder)
                                        if (selectedReminders.isEmpty()) exitSelectionMode()
                                    } else {
                                        viewingReminder = reminder
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedReminders.add(reminder)
                                    }
                                },
                                onDelete = { viewModel.deleteReminder(reminder) }
                            )
                        }
                    }
                }
            }

            if (showAddDialog) {
                ReminderEditDialog(
                    onDismiss = onAddDialogDismiss,
                    onConfirm = { reminder ->
                        viewModel.addReminder(reminder)
                        onAddDialogDismiss()
                    }
                )
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = onClearConfirmDismiss,
                    title = { Text("Clear All Reminders") },
                    text = { Text("Are you sure? This will delete all reminders and cancel their notifications.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllReminders()
                                onClearConfirmDismiss()
                            }
                        ) {
                            Text("Delete All", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onClearConfirmDismiss) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    viewModel = viewModel,
                    onDismiss = onSettingsDialogDismiss
                )
            }

            viewingReminder?.let { reminder ->
                ReminderDetailsDialog(
                    reminder = reminder,
                    onDismiss = { viewingReminder = null },
                    onEdit = {
                        editingReminder = reminder
                        viewingReminder = null
                    },
                    onDelete = {
                        viewModel.deleteReminder(reminder)
                        viewingReminder = null
                    }
                )
            }

            editingReminder?.let { reminder ->
                ReminderEditDialog(
                    reminder = reminder,
                    onDismiss = { editingReminder = null },
                    onConfirm = { updatedReminder ->
                        viewModel.updateReminder(updatedReminder)
                        editingReminder = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeToDeleteReminder(
    reminder: Reminder,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                MaterialTheme.colorScheme.error
            } else Color.Transparent

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            ReminderItem(reminder = reminder)
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == 1) "Set PIN" else "Confirm PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = if (step == 1) pin else confirmPin,
                    onValueChange = { 
                        if (it.all { char -> char.isDigit() } && it.length <= 8) {
                            if (step == 1) pin = it else confirmPin = it
                        }
                    },
                    label = { Text("Enter PIN (Digits only)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step == 1) {
                        if (pin.length < 4) {
                            error = "PIN must be at least 4 digits"
                        } else {
                            step = 2
                            error = null
                        }
                    } else {
                        if (confirmPin == pin) {
                            onConfirm(pin)
                        } else {
                            error = "PINs do not match"
                        }
                    }
                }
            ) {
                Text(if (step == 1) "Next" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsDialog(
    viewModel: ReminderViewModel,
    onDismiss: () -> Unit
) {
    val currentTheme by viewModel.appTheme.collectAsState()
    val currentColorTheme by viewModel.colorTheme.collectAsState()
    val isHiddenModeEnabled by viewModel.isHiddenModeEnabled.collectAsState()
    val context = LocalContext.current
    var showPinSetup by remember { mutableStateOf(false) }

    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin ->
                viewModel.savePin(pin)
                viewModel.setHiddenModeEnabled(true, context)
                showPinSetup = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("App Mode", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    AppTheme.entries.forEach { theme ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setAppTheme(theme) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (theme == currentTheme),
                                onClick = { viewModel.setAppTheme(theme) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = theme.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Color Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    ColorTheme.entries.forEach { theme ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setColorTheme(theme) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (theme == currentColorTheme),
                                onClick = { viewModel.setColorTheme(theme) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = theme.label)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Security", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Hidden App Mode", modifier = Modifier.weight(1f))
                    Switch(
                        checked = isHiddenModeEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showPinSetup = true
                            } else {
                                viewModel.setHiddenModeEnabled(false, context)
                            }
                        }
                    )
                }
                if (isHiddenModeEnabled) {
                    TextButton(onClick = { showPinSetup = true }) {
                        Text("Reset PIN")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ReminderItem(reminder: Reminder) {
    val isDark = isSystemInDarkTheme()
    val color = Color(if (isDark) reminder.color.dark else reminder.color.light)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Due: ${reminder.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm"))}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (reminder.isSmsEnabled && reminder.contacts.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = "SMS enabled",
                        modifier = Modifier.size(14.dp),
                        tint = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (reminder.contacts.isNotEmpty()) {
                    Text(
                        text = "To: ${reminder.contacts.joinToString { it.name }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderDetailsDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = reminder.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Row {
                    IconButton(onClick = {
                        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm")
                        val message = "Reminder: ${reminder.title}\n" +
                                     "Description: ${reminder.description}\n" +
                                     "Due Date: ${reminder.dateTime.format(formatter)}"
                        
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, message)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Send Reminder via")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (reminder.description.isNotBlank()) {
                    Text(text = reminder.description, style = MaterialTheme.typography.bodyLarge)
                }
                
                HorizontalDivider()

                DetailItem(label = "Date", value = reminder.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                DetailItem(label = "Time", value = reminder.dateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                DetailItem(label = "Recurrence", value = reminder.recurrence.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() })
                DetailItem(label = "Notifications", value = if (reminder.isNotificationEnabled) "Enabled" else "Disabled")
                
                if (reminder.isSmsEnabled && reminder.contacts.isNotEmpty()) {
                    DetailItem(label = "SMS Notification", value = "Enabled for ${reminder.contacts.joinToString { it.name }}")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Color: ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(if (isSystemInDarkTheme()) reminder.color.dark else reminder.color.light))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(reminder.color.label)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditDialog(
    reminder: Reminder? = null,
    onDismiss: () -> Unit,
    onConfirm: (Reminder) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var description by remember { mutableStateOf(reminder?.description ?: "") }
    var date by remember { mutableStateOf(reminder?.dateTime?.toLocalDate() ?: LocalDate.now()) }
    var time by remember { mutableStateOf(reminder?.dateTime?.toLocalTime() ?: LocalTime.now()) }
    var isNotificationEnabled by remember { mutableStateOf(reminder?.isNotificationEnabled ?: true) }
    var isSmsEnabled by remember { mutableStateOf(reminder?.isSmsEnabled ?: false) }
    var contacts by remember { mutableStateOf(reminder?.contacts ?: emptyList<ReminderContact>()) }
    var recurrence by remember { mutableStateOf(reminder?.recurrence ?: Recurrence.NONE) }
    var selectedColor by remember { mutableStateOf(reminder?.color ?: ReminderColor.GREY) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedRecurrence by remember { mutableStateOf(false) }
    
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            contactUri?.let { uri ->
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val number = cursor.getString(0)
                        val name = cursor.getString(1)
                        if (!contacts.any { it.phoneNumber == number }) {
                            contacts = contacts + ReminderContact(name, number)
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.SEND_SMS] == true && permissions[Manifest.permission.READ_CONTACTS] == true) {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(intent)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reminder == null) "Add Reminder" else "Edit Reminder") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp),
                    maxLines = 10
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Date: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}", modifier = Modifier.weight(1f))
                    Button(onClick = { showDatePicker = true }) { Text("Select Date") }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time: ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}", modifier = Modifier.weight(1f))
                    Button(onClick = { showTimePicker = true }) { Text("Select Time") }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Notifications", modifier = Modifier.weight(1f))
                    Switch(checked = isNotificationEnabled, onCheckedChange = { isNotificationEnabled = it })
                }

                HorizontalDivider()
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable SMS Reminders", modifier = Modifier.weight(1f))
                    Switch(checked = isSmsEnabled, onCheckedChange = { isSmsEnabled = it })
                }

                if (isSmsEnabled) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        contacts.forEach { contact ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${contact.name} (${contact.phoneNumber})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                IconButton(onClick = { contacts = contacts - contact }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Contact", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        
                        Button(
                            onClick = {
                                val smsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                                val contactPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                                
                                if (smsPerm == PackageManager.PERMISSION_GRANTED && contactPerm == PackageManager.PERMISSION_GRANTED) {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                    contactPickerLauncher.launch(intent)
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Contact (SMS)")
                        }
                    }
                }

                HorizontalDivider()

                ExposedDropdownMenuBox(
                    expanded = expandedRecurrence,
                    onExpandedChange = { expandedRecurrence = !expandedRecurrence }
                ) {
                    OutlinedTextField(
                        value = recurrence.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recurrence") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRecurrence) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRecurrence,
                        onDismissRequest = { expandedRecurrence = false }
                    ) {
                        Recurrence.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.replace("_", " ")) },
                                onClick = {
                                    recurrence = option
                                    expandedRecurrence = false
                                }
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Column(modifier = Modifier.fillMaxWidth()) {
                    val colors = ReminderColor.entries
                    val rows = colors.chunked(4)
                    rows.forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { colorOption ->
                                val isDark = isSystemInDarkTheme()
                                val color = Color(if (isDark) colorOption.dark else colorOption.light)
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor == colorOption) 3.dp else 1.dp,
                                            color = if (selectedColor == colorOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = colorOption }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            Reminder(
                                id = reminder?.id ?: 0,
                                title = title,
                                description = description,
                                dateTime = LocalDateTime.of(date, time),
                                isNotificationEnabled = isNotificationEnabled,
                                isSmsEnabled = isSmsEnabled,
                                contacts = contacts,
                                recurrence = recurrence,
                                color = selectedColor
                            )
                        )
                    }
                }
            ) {
                Text(if (reminder == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute
        )
        BasicAlertDialog(
            onDismissRequest = { showTimePicker = false }
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min).background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Select Time", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(20.dp))
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}
