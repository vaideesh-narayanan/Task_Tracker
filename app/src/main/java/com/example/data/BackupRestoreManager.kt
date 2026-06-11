package com.example.data

import android.content.Context
import android.net.Uri
import com.example.ui.TaskViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupRestoreManager(private val context: Context, private val viewModel: TaskViewModel) {
    private val gson = Gson()

    suspend fun exportTasks(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tasks = viewModel.tasks.first()
                val json = gson.toJson(tasks)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun importTasks(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<Task>>() {}.type
                    val tasks: List<Task> = gson.fromJson(json, type)
                    
                    tasks.forEach { task ->
                        viewModel.insertTask(
                            title = task.title,
                            description = task.description,
                            dueDateMillis = task.dueDateMillis,
                            priority = task.priority,
                            category = task.category
                        )
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
