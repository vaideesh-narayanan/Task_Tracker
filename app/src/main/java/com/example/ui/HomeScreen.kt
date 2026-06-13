package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.input.pointer.pointerInput
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
    val allActiveTasks by viewModel.allActiveTasks.collectAsState()
    val deletedTasks by viewModel.deletedTasks.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showStatsDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val backupRestoreManager = remember { com.example.data.BackupRestoreManager(context, viewModel) }

    val filters = listOf(TaskFilter.ALL, TaskFilter.PENDING, TaskFilter.COMPLETED, TaskFilter.EXPIRED)
    val filterLabels = listOf("All", "Pending", "Completed", "Expired")
    val pagerState = rememberPagerState(pageCount = { filters.size })

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
                if (isSearchActive) {
                    TopAppBar(
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search tasks...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("search_input")
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                isSearchActive = false
                                searchQuery = "" 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            actionIconContentColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                } else {
                    CenterAlignedTopAppBar(
                        title = { Text("Task Tracker", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search Tasks")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                            actionIconContentColor = MaterialTheme.colorScheme.primary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        actions = {
                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Tasks")
                            }
                        }
                    )
                }
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
                            selected = pagerState.currentPage == i,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(i)
                                }
                            },
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
                    val THIRTY_DAYS_IN_MILLIS = 30L * 24 * 60 * 60 * 1000L
                    val now = System.currentTimeMillis()
                    val pageTasks = remember(allActiveTasks, page) {
                        when (filters[page]) {
                            TaskFilter.ALL -> allActiveTasks.filter { !(it.isCompleted && it.completedAtMillis != null && now - it.completedAtMillis > THIRTY_DAYS_IN_MILLIS) }
                            TaskFilter.PENDING -> allActiveTasks.filter { !it.isCompleted }
                            TaskFilter.COMPLETED -> allActiveTasks.filter { it.isCompleted && (it.completedAtMillis == null || now - it.completedAtMillis <= THIRTY_DAYS_IN_MILLIS) }
                            TaskFilter.EXPIRED -> allActiveTasks.filter { !it.isCompleted && it.dueDateMillis != null && it.dueDateMillis < now }
                            TaskFilter.ARCHIVED -> emptyList()
                        }
                    }

                    val filteredTasks = remember(pageTasks, searchQuery) {
                        if (searchQuery.isBlank()) {
                            pageTasks
                        } else {
                            pageTasks.filter {
                                it.title.contains(searchQuery, ignoreCase = true) ||
                                it.description.contains(searchQuery, ignoreCase = true) ||
                                it.category.contains(searchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredTasks.isEmpty()) {
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
                                val emptyText = if (searchQuery.isNotEmpty()) {
                                    "No tasks found matching \"$searchQuery\""
                                } else {
                                    when (filters[page]) {
                                        TaskFilter.ALL -> "No tasks! Add your first task!"
                                        TaskFilter.PENDING -> "No pending tasks. Add one!"
                                        TaskFilter.COMPLETED -> "No completed tasks. Your completed tasks will appear here."
                                        TaskFilter.EXPIRED -> "No expired tasks. Your missed tasks will appear here."
                                        TaskFilter.ARCHIVED -> "No tasks here."
                                    }
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
                            items(filteredTasks, key = { "${page}_${it.id}" }) { task ->
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
                Text("Sort By", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
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
        TaskStatisticsDialog(allActiveTasks, onDismiss = { showStatsDialog = false })
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
    val expiredTasks = tasks.count { !it.isCompleted && it.dueDateMillis != null && it.dueDateMillis < System.currentTimeMillis() }
    val pendingTasks = tasks.count { !it.isCompleted } - expiredTasks
    val completionRate = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

    // Priority breakdown
    val highCount = tasks.count { it.priority == com.example.data.Priority.HIGH }
    val mediumCount = tasks.count { it.priority == com.example.data.Priority.MEDIUM }
    val lowCount = tasks.count { it.priority == com.example.data.Priority.LOW }

    // Category breakdown
    val categoryStats = remember(tasks) {
        tasks.groupBy { if (it.category.isBlank()) "Uncategorized" else it.category }
            .map { (cat, catTasks) ->
                val totalCat = catTasks.size
                val completedCat = catTasks.count { it.isCompleted }
                val rate = if (totalCat > 0) (completedCat * 100) / totalCat else 0
                Triple(cat, totalCat, rate)
            }
            .sortedByDescending { it.second }
    }

    val animatedTotal by animateIntAsState(targetValue = if (animationPlayed) totalTasks else 0, animationSpec = tween(1000), label = "total")
    val animatedCompleted by animateIntAsState(targetValue = if (animationPlayed) completedTasks else 0, animationSpec = tween(1000), label = "completed")
    val animatedPending by animateIntAsState(targetValue = if (animationPlayed) pendingTasks else 0, animationSpec = tween(1000), label = "pending")
    val animatedExpired by animateIntAsState(targetValue = if (animationPlayed) expiredTasks else 0, animationSpec = tween(1000), label = "expired")
    val animatedRate by animateIntAsState(targetValue = if (animationPlayed) completionRate else 0, animationSpec = tween(1000), label = "rate")

    var activeTab by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
        ) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp)
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Task Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))

                    // Pill tab selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Overview", "Categories & Priorities").forEachIndexed { index, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { activeTab = index }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeTab == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (activeTab == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Circular progress ring
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(90.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { animatedRate.toFloat() / 100f },
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        strokeWidth = 8.dp,
                                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$animatedRate%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Completed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                
                                Spacer(Modifier.width(20.dp))
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Overall Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("$completedTasks of $totalTasks tasks finished.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (expiredTasks > 0) {
                                        Text("$expiredTasks tasks expired!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatItem("Total", animatedTotal.toString(), MaterialTheme.colorScheme.primary)
                                StatItem("Pending", animatedPending.toString(), PriorityMedium)
                                StatItem("Completed", animatedCompleted.toString(), PriorityLow)
                                StatItem("Expired", animatedExpired.toString(), MaterialTheme.colorScheme.error)
                            }

                            Spacer(Modifier.height(20.dp))

                            val primaryColor = MaterialTheme.colorScheme.primary
                            val secondaryColor = PriorityLow
                            val tertiaryColor = PriorityMedium
                            val quaternaryColor = MaterialTheme.colorScheme.error

                            // Bar Chart Frame
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Status Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                                    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 8.dp)) {
                                        val maxVal = maxOf(totalTasks, 1)
                                        val width = size.width
                                        val height = size.height

                                        val barWidth = width * 0.16f
                                        val spacing = (width - (barWidth * 4)) / 3

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

                                        val expiredHeight = height * (animatedExpired / maxVal.toFloat())
                                        drawRect(
                                            color = quaternaryColor,
                                            topLeft = Offset((barWidth + spacing) * 3, height - expiredHeight),
                                            size = Size(barWidth, expiredHeight)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Priority Distribution
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Priority Distribution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    
                                    // Simple composite bar
                                    val totalPriority = maxOf(highCount + mediumCount + lowCount, 1).toFloat()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        if (highCount > 0) {
                                            Box(modifier = Modifier.weight(highCount / totalPriority).fillMaxHeight().background(PriorityHigh))
                                        }
                                        if (mediumCount > 0) {
                                            Box(modifier = Modifier.weight(mediumCount / totalPriority).fillMaxHeight().background(PriorityMedium))
                                        }
                                        if (lowCount > 0) {
                                            Box(modifier = Modifier.weight(lowCount / totalPriority).fillMaxHeight().background(PriorityLow))
                                        }
                                        if (highCount == 0 && mediumCount == 0 && lowCount == 0) {
                                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                                        }
                                    }
                                    
                                    Spacer(Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        PriorityLegendItem("High", highCount, PriorityHigh)
                                        PriorityLegendItem("Medium", mediumCount, PriorityMedium)
                                        PriorityLegendItem("Low", lowCount, PriorityLow)
                                    }
                                }
                            }

                            // Category List Breakdown
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Category Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(12.dp))
                                    
                                    if (categoryStats.isEmpty()) {
                                        Text("No categories specified", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        categoryStats.forEach { (catName, count, rate) ->
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(catName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                    Text("$count tasks ($rate% done)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { rate.toFloat() / 100f },
                                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                    color = if (rate >= 80) PriorityLow else if (rate >= 40) PriorityMedium else PriorityHigh,
                                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                                )
                                            }
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityLegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text("$label: $count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    modifier: Modifier = Modifier,
    isArchived: Boolean = false
) {
    var showPopup by remember { mutableStateOf(false) }

    val priorityColor = when (task.priority) {
        com.example.data.Priority.HIGH -> PriorityHigh
        com.example.data.Priority.MEDIUM -> PriorityMedium
        com.example.data.Priority.LOW -> PriorityLow
    }

    if (showPopup) {
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = { Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary) },
            text = { Text(if (isArchived) "You can only delete archived tasks." else "What would you like to do with this task?", color = MaterialTheme.colorScheme.primary) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {},
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (!isArchived) {
                        TextButton(onClick = { showPopup = false; onClick() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Edit", color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { 
                            showPopup = false
                            onCheckedChange(!task.isCompleted) 
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (task.isCompleted) "Mark as Pending" else "Mark as Completed", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TextButton(onClick = { showPopup = false; onDeleteClick() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { showPopup = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Row(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isArchived) {
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
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (task.category.isNotBlank()) {
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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
                    if (!isArchived) {
                        if (task.isCompleted) {
                            IconButton(onClick = { onCheckedChange(false) }) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Uncheck", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = onClick) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Box(
                modifier = Modifier.width(6.dp).fillMaxHeight()
                    .background(priorityColor)
                    .shadow(4.dp, spotColor = priorityColor, ambientColor = priorityColor)
            )
        }
    }
}
