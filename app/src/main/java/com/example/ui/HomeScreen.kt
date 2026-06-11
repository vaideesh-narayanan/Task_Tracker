package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Task
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToAddEdit: (Int?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToRecycleBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsState()
    val deletedTasks by viewModel.deletedTasks.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showStatsDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val backupRestoreManager = remember { com.example.data.BackupRestoreManager(context, viewModel) }

    val filters = listOf(TaskFilter.ALL, TaskFilter.PENDING, TaskFilter.COMPLETED, TaskFilter.EXPIRED)
    val filterLabels = listOf("All", "Pending", "Completed", "Expired")
    val pagerState = rememberPagerState(pageCount = { filters.size })

    LaunchedEffect(pagerState.currentPage) {
        if (currentFilter != TaskFilter.ARCHIVED) {
            viewModel.setFilter(filters[pagerState.currentPage])
        }
    }

    LaunchedEffect(currentFilter) {
        if (currentFilter != TaskFilter.ARCHIVED) {
            val idx = filters.indexOf(currentFilter)
            if (idx != -1 && pagerState.currentPage != idx) {
                pagerState.animateScrollToPage(idx)
            }
        }
    }

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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Task Tracker",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(12.dp))
                
                val menuItems = listOf(
                    "Task Statistics" to Icons.Filled.BarChart,
                    "Recycle Bin" to Icons.Filled.DeleteOutline,
                    "Archive" to Icons.Filled.Archive,
                    "Backup & Restore" to Icons.Filled.Backup,
                    "Settings" to Icons.Filled.Settings,
                    "About App" to Icons.Filled.Info
                )
                
                menuItems.forEachIndexed { index, pair ->
                    val (label, icon) = pair
                    var isVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 50L)
                        isVisible = true
                    }
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = slideInHorizontally(initialOffsetX = { -100 }) + fadeIn()
                    ) {
                        NavigationDrawerItem(
                            label = { Text(label) },
                            icon = { Icon(icon, contentDescription = null) },
                            badge = {
                                if (label == "Recycle Bin" && deletedTasks.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) { Text("${deletedTasks.size}") }
                                }
                            },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                when (label) {
                                    "Task Statistics" -> showStatsDialog = true
                                    "Recycle Bin" -> onNavigateToRecycleBin()
                                    "Archive" -> onNavigateToArchive()
                                    "Backup & Restore" -> showBackupRestoreDialog = true
                                    "Settings" -> onNavigateToSettings()
                                    "About App" -> onNavigateToAbout()
                                }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Task Tracker", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Tasks")
                        }
                    }
                )
            },
            floatingActionButton = {
                var isRotated by remember { mutableStateOf(false) }
                val rotationAngle by animateFloatAsState(targetValue = if (isRotated) 45f else 0f, label = "fabRotation")
                
                FloatingActionButton(
                    onClick = {
                        isRotated = !isRotated
                        scope.launch { delay(150); onNavigateToAddEdit(null) }
                    },
                    modifier = Modifier.rotate(rotationAngle).testTag("add_task_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Task")
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Filter Tabs (Pill shaped)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(TaskFilter.ALL, TaskFilter.PENDING, TaskFilter.COMPLETED, TaskFilter.EXPIRED)
                    val filterLabels = listOf("All", "Pending", "Completed", "Expired")
                    filters.forEachIndexed { i, filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filterLabels[i]) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) { page ->
                    if (tasks.isEmpty()) {
                        val infiniteTransition = rememberInfiniteTransition("bounce")
                        val bounce by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 20f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bounce"
                        )
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ListAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).offset(y = bounce.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(24.dp))
                                val emptyText = when (filters[page]) {
                                    TaskFilter.ALL -> "No tasks! Add your first task!"
                                    TaskFilter.PENDING -> "No pending tasks. Add one!"
                                    TaskFilter.COMPLETED -> "No completed tasks. Your completed tasks will appear here."
                                    TaskFilter.EXPIRED -> "No expired tasks. Your missed tasks will appear here."
                                    TaskFilter.ARCHIVED -> "No tasks here."
                                }
                                Text(
                                    text = emptyText, 
                                    style = MaterialTheme.typography.bodyLarge, 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tasks, key = { it.id }) { task ->
                                TaskItem(
                                    task = task,
                                    onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                    onClick = { onNavigateToAddEdit(task.id) },
                                    onDeleteClick = { viewModel.deleteTask(task.id) }
                                )
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sort By", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                ListItem(
                    headlineContent = { Text("Default") },
                    modifier = Modifier.clickable { viewModel.setSort(TaskSort.DEFAULT); showSortSheet = false }
                )
                ListItem(
                    headlineContent = { Text("Due Date (Earliest First)") },
                    modifier = Modifier.clickable { viewModel.setSort(TaskSort.DUE_DATE_ASC); showSortSheet = false }
                )
                ListItem(
                    headlineContent = { Text("Due Date (Latest First)") },
                    modifier = Modifier.clickable { viewModel.setSort(TaskSort.DUE_DATE_DESC); showSortSheet = false }
                )
                ListItem(
                    headlineContent = { Text("Recently Created") },
                    modifier = Modifier.clickable { viewModel.setSort(TaskSort.CREATION_DATE_DESC); showSortSheet = false }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
            text = { Text("Would you like to backup your tasks to a file, or restore tasks from a previous backup?") },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showBackupRestoreDialog = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    }) {
                        Text("Restore")
                    }
                    Button(onClick = {
                        showBackupRestoreDialog = false
                        exportLauncher.launch("tasks_backup.json")
                    }) {
                        Text("Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupRestoreDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showStatsDialog) {
        TaskStatisticsDialog(tasks, onDismiss = { showStatsDialog = false })
    }
}

@Composable
fun TaskStatisticsDialog(tasks: List<Task>, onDismiss: () -> Unit) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val pendingTasks = tasks.count { !it.isCompleted }

    val animatedTotal by animateIntAsState(targetValue = if (animationPlayed) totalTasks else 0, animationSpec = tween(1000), label = "total")
    val animatedCompleted by animateIntAsState(targetValue = if (animationPlayed) completedTasks else 0, animationSpec = tween(1000), label = "completed")
    val animatedPending by animateIntAsState(targetValue = if (animationPlayed) pendingTasks else 0, animationSpec = tween(1000), label = "pending")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Task Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Total", animatedTotal.toString(), MaterialTheme.colorScheme.primary)
                        StatItem("Pending", animatedPending.toString(), PriorityMedium)
                        StatItem("Completed", animatedCompleted.toString(), PriorityLow)
                    }

                    Spacer(Modifier.height(32.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = PriorityLow
                    val tertiaryColor = PriorityMedium

                    // Simple Bar Chart
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).padding(16.dp)) {
                        val maxVal = maxOf(totalTasks, 1) // prevent div by zero
                        val width = size.width
                        val height = size.height

                        val barWidth = width * 0.2f
                        val spacing = (width - (barWidth * 3)) / 2

                        val totalHeight = height * (animatedTotal / maxVal.toFloat())
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(0f, height - totalHeight),
                            size = Size(barWidth, totalHeight)
                        )

                        val pendingHeight = height * (animatedPending / maxVal.toFloat())
                        drawRect(
                            color = tertiaryColor,
                            topLeft = Offset(barWidth + spacing, height - pendingHeight),
                            size = Size(barWidth, pendingHeight)
                        )

                        val completedHeight = height * (animatedCompleted / maxVal.toFloat())
                        drawRect(
                            color = secondaryColor,
                            topLeft = Offset((barWidth + spacing) * 2, height - completedHeight),
                            size = Size(barWidth, completedHeight)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showPopup by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        com.example.data.Priority.HIGH -> PriorityHigh
        com.example.data.Priority.MEDIUM -> PriorityMedium
        com.example.data.Priority.LOW -> PriorityLow
    }

    if (showPopup) {
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = { Text("What would you like to do with this task?") },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showPopup = false; onClick() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Edit", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { 
                        showPopup = false
                        onCheckedChange(!task.isCompleted) 
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (task.isCompleted) "Mark as Pending" else "Mark as Completed", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { showPopup = false; onDeleteClick() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showPopup = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        )
    }

    var checkedScale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(targetValue = checkedScale, animationSpec = spring(), label = "scale")
    val alpha by animateFloatAsState(targetValue = if (task.isCompleted) 0.6f else 1f, label = "alpha")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showPopup = true }
            .graphicsLayer { this.alpha = alpha }
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier.width(6.dp).fillMaxHeight()
                    .background(priorityColor)
                    .shadow(4.dp, spotColor = priorityColor, ambientColor = priorityColor)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = {
                        checkedScale = 0.8f
                        onCheckedChange(it)
                        checkedScale = 1f
                    },
                    modifier = Modifier.scale(animatedScale).testTag("checkbox_${task.id}"),
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (task.category.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = task.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (task.dueDateMillis != null) {
                            val dateFormat = java.text.DateFormat.getDateTimeInstance(
                                java.text.DateFormat.MEDIUM,
                                java.text.DateFormat.SHORT
                            )
                            val isExpired = !task.isCompleted && task.dueDateMillis < System.currentTimeMillis()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = dateFormat.format(Date(task.dueDateMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (task.isCompleted) {
                        IconButton(onClick = { onCheckedChange(false) }) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Uncheck", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = onClick) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
