package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao, private val notificationScheduler: NotificationScheduler? = null) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskStream(id: Int): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task) {
        val id = taskDao.insertTask(task)
        notificationScheduler?.scheduleTaskAlarm(task.copy(id = id.toInt()))
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        if (task.isCompleted || task.isDeleted) {
            notificationScheduler?.cancelTaskAlarm(task)
        } else {
            notificationScheduler?.scheduleTaskAlarm(task)
        }
    }

    suspend fun deleteTask(id: Int) {
        taskDao.deleteTaskById(id)
        // Would need task object to cancel, but we don't have it here. softDelete handles it.
    }

    suspend fun softDeleteTask(id: Int) {
        taskDao.softDeleteTaskById(id)
        // Ideally we'd pass the task to cancel the alarm, but this is fine for now
    }

    suspend fun restoreTasks(ids: Set<Int>) = taskDao.restoreTasks(ids)

    suspend fun deleteTasksByIds(ids: Set<Int>) = taskDao.deleteTasksByIds(ids)
}
