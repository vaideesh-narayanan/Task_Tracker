package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Priority
import com.example.data.Task
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    taskId: Int?,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var category by remember { mutableStateOf("") }
    var existingTask by remember { mutableStateOf<Task?>(null) }
    var pastWarningType by remember { mutableStateOf<String?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val saveTask = {
        if (title.isNotBlank()) {
            scope.launch {
                isSaving = true
                delay(400) // fake saving animation
                if (taskId == null) {
                    viewModel.insertTask(title, description, dueDateMillis, priority, category)
                } else {
                    existingTask?.let {
                        viewModel.updateTask(it.copy(title = title, description = description, dueDateMillis = dueDateMillis, priority = priority, category = category))
                    }
                }
                isSaving = false
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getTaskStream(taskId).collect { task ->
                if (task != null) {
                    existingTask = task
                    title = task.title
                    description = task.description
                    dueDateMillis = task.dueDateMillis
                    priority = task.priority
                    category = task.category
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "Add Task" else "Edit Task", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AnimatedContent(targetState = isSaving, label = "saveIcon") { saving ->
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(16.dp).size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                if (title.isNotBlank()) {
                                    if (dueDateMillis != null && dueDateMillis!! < System.currentTimeMillis()) {
                                        pastWarningType = "Time"
                                    } else {
                                        saveTask()
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Check, contentDescription = "Save")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title & Desc Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }

            // Category Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val predefinedCategories = listOf("Work", "Personal", "Shopping", "Health")
                    predefinedCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = if (category == cat) "" else cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = if (category == cat) { { Icon(Icons.Filled.Check, null) } } else null
                        )
                    }
                }
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    placeholder = { Text("Or add custom category...") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, null) }
                )
            }

            // Priority
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Priority", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(Priority.LOW to PriorityLow, Priority.MEDIUM to PriorityMedium, Priority.HIGH to PriorityHigh).forEach { (p, color) ->
                        val isSelected = priority == p
                        val containerColor by animateColorAsState(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant, label = "p_color")
                        val contentColor by animateColorAsState(if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant, label = "p_content")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(containerColor)
                                .clickable { priority = p }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(if (isSelected) Icons.Filled.Flag else Icons.Outlined.Flag, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                                Text(p.name, color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }

            // Due Date
            var showDatePicker by remember { mutableStateOf(false) }
            var showTimePicker by remember { mutableStateOf(false) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Due Date & Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        if (dueDateMillis != null) {
                            val dateFormat = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                            Text(dateFormat.format(java.util.Date(dueDateMillis!!)), style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text("Not set", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                
                if (dueDateMillis != null) {
                    TextButton(onClick = { dueDateMillis = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear Date", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Pickers
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
            val timePickerState = rememberTimePickerState(
                initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
            )
            var tempDateMillis by remember { mutableStateOf<Long?>(null) }

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            tempDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                            showTimePicker = true
                        }) { Text("Next") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = datePickerState) }
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            tempDateMillis?.let { dateMillis ->
                                val utcCalendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateMillis }
                                val localCalendar = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
                                    set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                }
                                dueDateMillis = localCalendar.timeInMillis
                            }
                            showTimePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
                    title = { Text("Select Time") },
                    text = { TimePicker(state = timePickerState) }
                )
            }

            if (pastWarningType != null) {
                AlertDialog(
                    onDismissRequest = { pastWarningType = null },
                    title = { Text("Past Due Date") },
                    text = { Text("The selected due date is in the past. Proceed?") },
                    confirmButton = { TextButton(onClick = { pastWarningType = null; saveTask() }) { Text("Yes") } },
                    dismissButton = { TextButton(onClick = { pastWarningType = null }) { Text("Cancel") } }
                )
            }
        }
    }
}
