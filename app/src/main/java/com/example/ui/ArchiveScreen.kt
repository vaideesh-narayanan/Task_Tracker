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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No archived tasks", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
