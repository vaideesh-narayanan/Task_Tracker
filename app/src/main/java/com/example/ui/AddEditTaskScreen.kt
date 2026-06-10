package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Task
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
    var priority by remember { mutableStateOf(com.example.data.Priority.MEDIUM) }
    var category by remember { mutableStateOf("") }
    var existingTask by remember { mutableStateOf<Task?>(null) }
    var pastWarningType by remember { mutableStateOf<String?>(null) }

    val saveTask = {
        if (title.isNotBlank()) {
            if (taskId == null) {
                viewModel.insertTask(title, description, dueDateMillis, priority, category)
            } else {
                existingTask?.let {
                    viewModel.updateTask(it.copy(title = title, description = description, dueDateMillis = dueDateMillis, priority = priority, category = category))
                }
            }
            onNavigateBack()
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
                title = { Text(if (taskId == null) "Add Task" else "Edit Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (title.isNotBlank()) {
                        if (dueDateMillis != null && dueDateMillis!! < System.currentTimeMillis()) {
                            val now = System.currentTimeMillis()
                            val todayCalendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
                            val dueCalendar = java.util.Calendar.getInstance().apply { timeInMillis = dueDateMillis!! }
                            
                            val isPastDate = dueCalendar.get(java.util.Calendar.YEAR) < todayCalendar.get(java.util.Calendar.YEAR) ||
                                (dueCalendar.get(java.util.Calendar.YEAR) == todayCalendar.get(java.util.Calendar.YEAR) && dueCalendar.get(java.util.Calendar.DAY_OF_YEAR) < todayCalendar.get(java.util.Calendar.DAY_OF_YEAR))
                                
                            if (isPastDate) {
                                pastWarningType = "Date"
                            } else {
                                pastWarningType = "Time"
                            }
                        } else {
                            saveTask()
                        }
                    }
                },
                modifier = Modifier.testTag("save_task_fab")
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Save Task")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("title_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("description_input"),
                maxLines = 5
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (e.g. Work, Personal)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Priority", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.data.Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.name) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            var showDatePicker by remember { mutableStateOf(false) }
            var showTimePicker by remember { mutableStateOf(false) }
            var tempSelectedDateMillis by remember { mutableStateOf<Long?>(null) }
            
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
            val timePickerState = rememberTimePickerState(
                initialHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                initialMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            tempSelectedDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                            showTimePicker = true
                        }) {
                            Text("Next")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Time") },
                    text = {
                        TimePicker(state = timePickerState)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            tempSelectedDateMillis?.let { dateMillis ->
                                val utcCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                                utcCalendar.timeInMillis = dateMillis
                                
                                val localCalendar = java.util.Calendar.getInstance()
                                localCalendar.set(java.util.Calendar.YEAR, utcCalendar.get(java.util.Calendar.YEAR))
                                localCalendar.set(java.util.Calendar.MONTH, utcCalendar.get(java.util.Calendar.MONTH))
                                localCalendar.set(java.util.Calendar.DAY_OF_MONTH, utcCalendar.get(java.util.Calendar.DAY_OF_MONTH))
                                localCalendar.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                                localCalendar.set(java.util.Calendar.MINUTE, timePickerState.minute)
                                localCalendar.set(java.util.Calendar.SECOND, 0)
                                
                                dueDateMillis = localCalendar.timeInMillis
                            }
                            showTimePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Button(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                val buttonText = if (dueDateMillis != null) {
                    val dateFormat = java.text.DateFormat.getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.SHORT
                    )
                    "Due Date: ${dateFormat.format(java.util.Date(dueDateMillis!!))}"
                } else {
                    "Set Due Date & Time"
                }
                Text(buttonText)
            }
            
            if (dueDateMillis != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { dueDateMillis = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Due Date")
                }
            }
            
            if (pastWarningType != null) {
                AlertDialog(
                    onDismissRequest = { pastWarningType = null },
                    title = { Text(if (pastWarningType == "Date") "Past Due Date" else "Past Due Time") },
                    text = { Text("The selected due ${pastWarningType?.lowercase()} is in the past. Do you want to proceed anyway or fix the date/time?") },
                    confirmButton = {
                        TextButton(onClick = {
                            pastWarningType = null
                            saveTask()
                        }) {
                            Text("Proceed Anyway")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pastWarningType = null }) {
                            Text("Fix Date/Time")
                        }
                    }
                )
            }
        }
    }
}
