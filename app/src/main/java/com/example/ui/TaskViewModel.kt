package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter { ALL, PENDING, COMPLETED, EXPIRED, ARCHIVED }
enum class TaskSort { DEFAULT, DUE_DATE_ASC, DUE_DATE_DESC, CREATION_DATE_DESC }

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    private val _currentSort = MutableStateFlow(TaskSort.DEFAULT)
    val currentSort: StateFlow<TaskSort> = _currentSort.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _currentFilter,
        _currentSort
    ) { taskList, filter, sort ->
        val now = System.currentTimeMillis()
        val THIRTY_DAYS_IN_MILLIS = 30L * 24 * 60 * 60 * 1000L
        val filtered = when (filter) {
            TaskFilter.ALL -> taskList.filter { !(it.isCompleted && it.completedAtMillis != null && now - it.completedAtMillis > THIRTY_DAYS_IN_MILLIS) }
            TaskFilter.PENDING -> taskList.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> taskList.filter { it.isCompleted && (it.completedAtMillis == null || now - it.completedAtMillis <= THIRTY_DAYS_IN_MILLIS) }
            TaskFilter.EXPIRED -> taskList.filter { !it.isCompleted && it.dueDateMillis != null && it.dueDateMillis < now }
            TaskFilter.ARCHIVED -> taskList.filter { it.isCompleted && it.completedAtMillis != null && now - it.completedAtMillis > THIRTY_DAYS_IN_MILLIS }
        }

        when (sort) {
            TaskSort.DEFAULT -> filtered.sortedWith(compareBy({ it.isCompleted }, { it.dueDateMillis ?: Long.MAX_VALUE }, { -it.createdAtMillis }))
            TaskSort.DUE_DATE_ASC -> filtered.sortedWith(compareBy({ it.dueDateMillis == null }, { it.dueDateMillis }))
            TaskSort.DUE_DATE_DESC -> filtered.sortedWith(compareBy({ it.dueDateMillis == null }, { -(it.dueDateMillis ?: 0L) }))
            TaskSort.CREATION_DATE_DESC -> filtered.sortedByDescending { it.createdAtMillis }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setFilter(filter: TaskFilter) { _currentFilter.value = filter }
    fun setSort(sort: TaskSort) { _currentSort.value = sort }

    fun getTaskStream(id: Int) = repository.getTaskStream(id)

    fun insertTask(title: String, description: String, dueDateMillis: Long?, priority: com.example.data.Priority, category: String) {
        viewModelScope.launch {
            repository.insertTask(
                Task(title = title, description = description, dueDateMillis = dueDateMillis, priority = priority, category = category)
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val isCompleted = !task.isCompleted
            val completedAt = if (isCompleted) System.currentTimeMillis() else null
            repository.updateTask(task.copy(isCompleted = isCompleted, completedAtMillis = completedAt))
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    class Factory(private val repository: TaskRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
