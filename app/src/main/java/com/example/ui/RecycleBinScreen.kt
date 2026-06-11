package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Task
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val deletedTasks by viewModel.deletedTasks.collectAsState()
    var selectedTaskIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showConfirmDeleteDialog by remember { mutableStateOf<Set<Int>?>(null) } // Set of task IDs to delete

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedTasks.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedTaskIds = if (selectedTaskIds.size == deletedTasks.size) {
                                    emptySet()
                                } else {
                                    deletedTasks.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select All")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (deletedTasks.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedTaskIds.size} selected", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    viewModel.restoreTasks(selectedTaskIds)
                                    selectedTaskIds = emptySet()
                                },
                                enabled = selectedTaskIds.isNotEmpty()
                            ) {
                                Text("Restore")
                            }
                            Button(
                                onClick = { showConfirmDeleteDialog = selectedTaskIds },
                                enabled = selectedTaskIds.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (deletedTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Recycle Bin is empty", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(deletedTasks) { task ->
                        RecycleBinTaskItem(
                            task = task,
                            isSelected = selectedTaskIds.contains(task.id),
                            onCheckedChange = { isChecked ->
                                selectedTaskIds = if (isChecked) {
                                    selectedTaskIds + task.id
                                } else {
                                    selectedTaskIds - task.id
                                }
                            },
                            onRestore = { viewModel.restoreTasks(setOf(task.id)) },
                            onDelete = { showConfirmDeleteDialog = setOf(task.id) }
                        )
                    }
                }
            }
        }
    }

    showConfirmDeleteDialog?.let { taskIdsToDelete ->
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = null },
            title = { Text("Permanently Delete") },
            text = { Text("Are you sure you want to permanently delete the selected task(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.permanentlyDeleteTasks(taskIdsToDelete)
                        selectedTaskIds = selectedTaskIds - taskIdsToDelete
                        showConfirmDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecycleBinTaskItem(
    task: Task,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                checked = isSelected,
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.dueDateMillis != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateFormat = java.text.DateFormat.getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.SHORT
                    )
                    Text(
                        text = "Due: ${dateFormat.format(Date(task.dueDateMillis))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Filled.Restore, contentDescription = "Restore Task", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
