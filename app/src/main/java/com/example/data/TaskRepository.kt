package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskStream(id: Int): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(id: Int) = taskDao.deleteTaskById(id)

    suspend fun softDeleteTask(id: Int) = taskDao.softDeleteTaskById(id)

    suspend fun restoreTasks(ids: Set<Int>) = taskDao.restoreTasks(ids)

    suspend fun deleteTasksByIds(ids: Set<Int>) = taskDao.deleteTasksByIds(ids)
}
