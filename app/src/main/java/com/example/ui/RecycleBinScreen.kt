package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val deletedTasks by viewModel.deletedTasks.collectAsState()
    var selectedTaskIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showConfirmDeleteDialog by remember { mutableStateOf<Set<Int>?>(null) } 
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    if (deletedTasks.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedTaskIds = if (selectedTaskIds.size == deletedTasks.size) {
                                    emptySet()
                                } else {
                                    deletedTasks.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Text(if (selectedTaskIds.size == deletedTasks.size) "Deselect All" else "Select All")
                        }
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selectedTaskIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = shrinkVertically()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedTaskIds.size} selected", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.restoreTasks(selectedTaskIds)
                                        selectedTaskIds = emptySet()
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Restore")
                            }
                            TextButton(
                                onClick = { showConfirmDeleteDialog = selectedTaskIds }
                            ) {
                                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(deletedTasks, key = { it.id }) { task ->
                        var isDismissing by remember { mutableStateOf(false) }
                        var dismissDirection by remember { mutableStateOf(0) } // 1 for restore, -1 for delete

                        AnimatedVisibility(
                            visible = !isDismissing,
                            exit = if (dismissDirection == 1) slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it })
                                   else shrinkVertically(animationSpec = tween(300)) + fadeOut(),
                            modifier = Modifier.animateItem()
                        ) {
                            RecycleBinTaskItem(
                                task = task,
                                isSelected = selectedTaskIds.contains(task.id),
                                onCheckedChange = { isChecked ->
                                    selectedTaskIds = if (isChecked) selectedTaskIds + task.id else selectedTaskIds - task.id
                                },
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedTaskIds = if (selectedTaskIds.contains(task.id)) selectedTaskIds - task.id else selectedTaskIds + task.id
                                },
                                onRestore = {
                                    dismissDirection = 1
                                    isDismissing = true
                                    scope.launch {
                                        delay(300)
                                        viewModel.restoreTasks(setOf(task.id))
                                        selectedTaskIds = selectedTaskIds - task.id
                                    }
                                },
                                onDelete = { showConfirmDeleteDialog = setOf(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    showConfirmDeleteDialog?.let { taskIdsToDelete ->
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = null },
            title = { Text("Permanently Delete", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete the selected task(s)? This action cannot be undone.") },
            containerColor = MaterialTheme.colorScheme.surface,
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onLongPress: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(task.id) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onCheckedChange(!isSelected) }
                )
            }
            .alpha(0.7f), // Faded style to indicate deleted
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = (onCheckedChange),
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (task.category.isNotBlank()) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (task.dueDateMillis != null) {
                        val dateFormat = java.text.DateFormat.getDateTimeInstance(
                            java.text.DateFormat.MEDIUM,
                            java.text.DateFormat.SHORT
                        )
                        Text(
                            text = "Due: ${dateFormat.format(Date(task.dueDateMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Filled.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
