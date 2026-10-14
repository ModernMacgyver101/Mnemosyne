package com.hardlinecoding.checkthebox.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hardlinecoding.checkthebox.model.Priority
import com.hardlinecoding.checkthebox.model.ToDoItem
import com.hardlinecoding.checkthebox.model.PrefilledTask
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ToDoScreen(
    reminderViewModel: ReminderViewModel,
    viewModel: ToDoViewModel = viewModel(),
    showAddDialog: Boolean,
    onAddDialogDismiss: () -> Unit,
    showSettingsDialog: Boolean,
    onSettingsDialogDismiss: () -> Unit,
    showClearConfirm: Boolean,
    onClearConfirmDismiss: () -> Unit
) {
    val toDoItems by viewModel.toDoItems.collectAsState()

    var editingItem by remember { mutableStateOf<ToDoItem?>(null) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<ToDoItem>() }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TopAppBar(
                    title = { Text("${selectedItems.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            selectedItems.clear()
                            selectedItems.addAll(toDoItems)
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            selectedItems.forEach { viewModel.deleteToDoItem(it) }
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
            val groupedByStatus = toDoItems.groupBy { it.isCompleted }
            val activeItems = groupedByStatus[false] ?: emptyList()
            val completedItems = groupedByStatus[true] ?: emptyList()

            val groupedByPriority = activeItems.groupBy { it.priority }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp, start = 8.dp, end = 8.dp)
            ) {
                // Pinned Items Section (Always at top)
                val pinnedItems = activeItems.filter { it.isPinned }
                if (pinnedItems.isNotEmpty()) {
                    item {
                        Text(
                            "Pinned",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(pinnedItems, key = { "pinned_${it.id}" }) { item ->
                        ToDoListItem(
                            item = item,
                            isSelected = selectedItems.contains(item),
                            isSelectionMode = isSelectionMode,
                            onToggle = { viewModel.updateToDoItem(item.copy(isCompleted = !item.isCompleted)) },
                            onPinToggle = { viewModel.updateToDoItem(item.copy(isPinned = !item.isPinned)) },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedItems.add(item)
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedItems.contains(item)) selectedItems.remove(item)
                                    else selectedItems.add(item)
                                    if (selectedItems.isEmpty()) exitSelectionMode()
                                } else {
                                    editingItem = item
                                }
                            }
                        )
                    }
                }

                // Priority Sections
                Priority.entries.forEach { priority ->
                    val itemsInPriority = groupedByPriority[priority]?.filter { !it.isPinned } ?: emptyList()
                    if (itemsInPriority.isNotEmpty()) {
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            ) {
                                Text(
                                    priority.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when(priority) {
                                        Priority.IMMEDIATE -> MaterialTheme.colorScheme.error
                                        Priority.URGENT -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        Priority.PRIORITY -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                        items(itemsInPriority, key = { it.id }) { item ->
                            ToDoListItem(
                                item = item,
                                isSelected = selectedItems.contains(item),
                                isSelectionMode = isSelectionMode,
                                onToggle = { viewModel.updateToDoItem(item.copy(isCompleted = !item.isCompleted)) },
                                onPinToggle = { viewModel.updateToDoItem(item.copy(isPinned = !item.isPinned)) },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedItems.add(item)
                                    }
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        if (selectedItems.contains(item)) selectedItems.remove(item)
                                        else selectedItems.add(item)
                                        if (selectedItems.isEmpty()) exitSelectionMode()
                                    } else {
                                        editingItem = item
                                    }
                                }
                            )
                        }
                    }
                }

                // Completed Section
                if (completedItems.isNotEmpty()) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Column {
                            TextButton(onClick = { expanded = !expanded }) {
                                Text("Completed (${completedItems.size})")
                                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                            }
                            AnimatedVisibility(visible = expanded) {
                                Column {
                                    completedItems.forEach { item ->
                                        ToDoListItem(
                                            item = item,
                                            isSelected = selectedItems.contains(item),
                                            isSelectionMode = isSelectionMode,
                                            onToggle = { viewModel.updateToDoItem(item.copy(isCompleted = !item.isCompleted)) },
                                            onPinToggle = { viewModel.updateToDoItem(item.copy(isPinned = !item.isPinned)) },
                                            onLongClick = { /* No long click for completed */ },
                                            onClick = {
                                                if (isSelectionMode) {
                                                    if (selectedItems.contains(item)) selectedItems.remove(item)
                                                    else selectedItems.add(item)
                                                    if (selectedItems.isEmpty()) exitSelectionMode()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ToDoEditDialog(
            onDismiss = onAddDialogDismiss,
            onConfirm = { title, desc, isPinned, priority ->
                viewModel.addToDoItem(ToDoItem(title = title, description = desc, isPinned = isPinned, priority = priority))
                onAddDialogDismiss()
            }
        )
    }

    editingItem?.let { item ->
        ToDoEditDialog(
            item = item,
            onDismiss = { editingItem = null },
            onConfirm = { title, desc, isPinned, priority ->
                viewModel.updateToDoItem(
                    item.copy(title = title, description = desc, isPinned = isPinned, priority = priority)
                )
                editingItem = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = onClearConfirmDismiss,
            title = { Text("Clear All Items") },
            text = { Text("Are you sure? This will delete all to-do items.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllToDoItems()
                        onClearConfirmDismiss()
                    }
                ) { Text("Delete All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = onClearConfirmDismiss) { Text("Cancel") } }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            viewModel = reminderViewModel,
            onDismiss = onSettingsDialogDismiss
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToDoListItem(
    item: ToDoItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: () -> Unit,
    onPinToggle: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val textColor = if (item.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
    val textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
            .padding(vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Complete",
                    tint = if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = textColor, textDecoration = textDecoration, fontWeight = if (item.isPinned) FontWeight.Bold else FontWeight.Normal)
                if (item.description.isNotBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = textColor, textDecoration = textDecoration)
                }
            }
            IconButton(onClick = onPinToggle) {
                Icon(
                    if (item.isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Pin",
                    tint = if (item.isPinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoEditDialog(
    item: ToDoItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean, Priority) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(item?.title ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var isPinned by remember { mutableStateOf(item?.isPinned ?: false) }
    var priority by remember { mutableStateOf(item?.priority ?: Priority.ROUTINE) }
    
    var showPrefilledTasksManager by remember { mutableStateOf(false) }
    var prefilledTasks by remember { mutableStateOf(getPrefilledTasks(context)) }

    if (showPrefilledTasksManager) {
        PrefilledTasksManagerDialog(
            tasks = prefilledTasks,
            onDismiss = { showPrefilledTasksManager = false },
            onUpdate = { updatedTasks ->
                savePrefilledTasks(context, updatedTasks)
                prefilledTasks = updatedTasks
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (item == null) "Add To-Do" else "Edit To-Do")
                IconButton(onClick = { showPrefilledTasksManager = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Manage Prefilled Tasks")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item == null) {
                    Text("Prefilled Tasks", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        prefilledTasks.forEach { task ->
                            AssistChip(
                                onClick = { 
                                    title = task.title
                                    description = task.description
                                },
                                label = { Text(task.title) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

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
                    modifier = Modifier.heightIn(max = 150.dp).fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pinned", modifier = Modifier.weight(1f))
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
                var menuExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = menuExpanded,
                    onExpandedChange = { menuExpanded = !menuExpanded }
                ) {
                    OutlinedTextField(
                        value = priority.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        Priority.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.label) }, onClick = { priority = p; menuExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description, isPinned, priority) }
            ) { Text(if (item == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PrefilledTasksManagerDialog(
    tasks: List<PrefilledTask>,
    onDismiss: () -> Unit,
    onUpdate: (List<PrefilledTask>) -> Unit
) {
    var currentTasks by remember { mutableStateOf(tasks) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Prefilled Tasks") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("New Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        label = { Text("New Task Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newTaskTitle.isNotBlank()) {
                                currentTasks = currentTasks + PrefilledTask(newTaskTitle.trim(), newTaskDesc.trim())
                                newTaskTitle = ""
                                newTaskDesc = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add Task")
                    }
                }
                
                HorizontalDivider()
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(currentTasks) { task ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold)
                                if (task.description.isNotBlank()) {
                                    Text(task.description, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = {
                                currentTasks = currentTasks - task
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onUpdate(currentTasks)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun getPrefilledTasks(context: Context): List<PrefilledTask> {
    val prefs = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    val json = prefs.getString("prefilled_tasks", null)
    return if (json != null) {
        val type = object : TypeToken<List<PrefilledTask>>() {}.type
        Gson().fromJson(json, type)
    } else {
        listOf(
            PrefilledTask("haze the enlisted", "standard disciplinary procedure"),
            PrefilledTask("attend op sync", "mandatory daily briefing"),
            PrefilledTask("cry in corner", "personal development time")
        )
    }
}

private fun savePrefilledTasks(context: Context, tasks: List<PrefilledTask>) {
    val prefs = context.getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
    prefs.edit { 
        putString("prefilled_tasks", Gson().toJson(tasks))
    }
}
