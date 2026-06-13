package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddEdit: (Int) -> Unit
) {
    val archivedTasks by viewModel.archivedTasks.collectAsState()
    val scope = rememberCoroutineScope()
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Filled.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("About Archive", color = MaterialTheme.colorScheme.primary) },
            text = {
                Text(
                    "Tasks completed more than 30 days ago are automatically moved here to keep your main workspace organized. They are kept for your history and records.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (archivedTasks.isNotEmpty()) {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "Archive Info")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (archivedTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Archive,
                            contentDescription = "Archive",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No archived tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tasks completed more than 30 days ago are automatically moved here. They are hidden from the main view but kept for your records. You can also view them to track your past accomplishments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(archivedTasks, key = { it.id }) { task ->
                        var isDismissing by remember { mutableStateOf(false) }

                        AnimatedVisibility(
                            visible = !isDismissing,
                            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(),
                            modifier = Modifier.animateItem()
                        ) {
                            TaskItem(
                                task = task,
                                onCheckedChange = {
                                    isDismissing = true
                                    scope.launch {
                                        delay(300)
                                        viewModel.toggleTaskCompletion(task)
                                    }
                                },
                                onClick = { onNavigateToAddEdit(task.id) },
                                onDeleteClick = { 
                                    isDismissing = true
                                    scope.launch {
                                        delay(300)
                                        viewModel.deleteTask(task.id) 
                                    }
                                },
                                isArchived = true
                            )
                        }
                    }
                }
            }
        }
    }
}
