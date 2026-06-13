package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val database = AppDatabase.getDatabase(context)
            val notificationScheduler = NotificationScheduler(context)
            val repo = TaskRepository(database.taskDao(), notificationScheduler)
            
            val settingsManager = SettingsManager(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val notificationsEnabled = settingsManager.notificationsEnabledFlow.first()
                if (notificationsEnabled) {
                    val activeTasks = repo.allTasks.first()
                    activeTasks.forEach { task ->
                        if (!task.isCompleted && !task.isDeleted && task.dueDateMillis != null && task.dueDateMillis > System.currentTimeMillis()) {
                            notificationScheduler.scheduleTaskAlarm(task)
                        }
                    }
                }
            }
        }
    }
}
