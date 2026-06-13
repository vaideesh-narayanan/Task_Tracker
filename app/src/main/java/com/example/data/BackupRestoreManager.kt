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
                // Export all active and archived tasks regardless of current filter
                val tasks = viewModel.allActiveTasks.first() + viewModel.archivedTasks.first()
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
                        // Re-insert exact task state (if importing back, we want to maintain completion state, etc)
                        // If ID exists it might conflict, inserting as 0 generates a new ID but maintains state
                        viewModel.insertTaskDirectly(task.copy(id = 0)) 
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
