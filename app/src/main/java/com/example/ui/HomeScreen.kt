package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Menu
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToAddEdit: (Int?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showStatsDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val backupRestoreManager = remember { com.example.data.BackupRestoreManager(context, viewModel) }
    
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = backupRestoreManager.exportTasks(uri)
                val msg = if (success) "Export successful" else "Export failed"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = backupRestoreManager.importTasks(uri)
                val msg = if (success) "Import successful" else "Import failed"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Task Tracker",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Task Statistics") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showStatsDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Backup & Restore") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showBackupRestoreDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Recycle Bin") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToRecycleBin()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Privacy Policy") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToPrivacyPolicy()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Tracker") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort Tasks")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Default") },
                            onClick = { viewModel.setSort(TaskSort.DEFAULT); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Due Date (Earliest First)") },
                            onClick = { viewModel.setSort(TaskSort.DUE_DATE_ASC); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Due Date (Latest First)") },
                            onClick = { viewModel.setSort(TaskSort.DUE_DATE_DESC); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Recently Created") },
                            onClick = { viewModel.setSort(TaskSort.CREATION_DATE_DESC); showSortMenu = false }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEdit(null) },
                modifier = Modifier.testTag("add_task_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentFilter == TaskFilter.ALL,
                    onClick = { viewModel.setFilter(TaskFilter.ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.PENDING,
                    onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.COMPLETED,
                    onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                    label = { Text("Completed") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.EXPIRED,
                    onClick = { viewModel.setFilter(TaskFilter.EXPIRED) },
                    label = { Text("Expired") }
                )
                FilterChip(
                    selected = currentFilter == TaskFilter.ARCHIVED,
                    onClick = { viewModel.setFilter(TaskFilter.ARCHIVED) },
                    label = { Text("Archive") }
                )
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                            onClick = { onNavigateToAddEdit(task.id) },
                            onDeleteClick = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
    } // End of ModalNavigationDrawer

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("Backup & Restore") },
            text = { Text("Would you like to backup your tasks to a file, or restore tasks from a previous backup?") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showBackupRestoreDialog = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Text("Restore")
                    }
                    TextButton(onClick = {
                        showBackupRestoreDialog = false
                        exportLauncher.launch("tasks_backup.json")
                    }) {
                        Text("Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showStatsDialog) {
        val totalTasks = tasks.size
        val pendingTasks = tasks.count { !it.isCompleted }
        val completedTasks = tasks.count { it.isCompleted }
        
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Task Statistics") },
            text = {
                Column {
                    Text("Total Tasks: $totalTasks")
                    Text("Pending Tasks: $pendingTasks")
                    Text("Completed Tasks: $completedTasks")
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
            .testTag("task_item_card_${task.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag("checkbox_${task.id}")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val priorityColor = when (task.priority) {
                        com.example.data.Priority.HIGH -> MaterialTheme.colorScheme.error
                        com.example.data.Priority.MEDIUM -> MaterialTheme.colorScheme.primary
                        com.example.data.Priority.LOW -> MaterialTheme.colorScheme.secondary
                    }
                    Surface(color = priorityColor.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small) {
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    if (task.category.isNotBlank()) {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                if (task.dueDateMillis != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormat = java.text.DateFormat.getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.SHORT
                    )
                    val isExpired = !task.isCompleted && task.dueDateMillis < System.currentTimeMillis()
                    Text(
                        text = "Due: ${dateFormat.format(Date(task.dueDateMillis))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.testTag("delete_${task.id}")) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
