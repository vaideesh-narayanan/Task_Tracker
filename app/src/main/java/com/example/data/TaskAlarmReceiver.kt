package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.content.pm.PackageManager
import android.Manifest
import com.example.MainActivity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.core.net.toUri

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getIntExtra("TASK_ID", -1)
        
        if (action == "MARK_COMPLETED" && taskId != -1) {
            val database = AppDatabase.getDatabase(context)
            val repo = TaskRepository(database.taskDao(), NotificationScheduler(context))
            runBlocking {
                val task = repo.getTaskStream(taskId).first()
                if (task != null) {
                    repo.updateTask(task.copy(isCompleted = true))
                }
            }
            with(NotificationManagerCompat.from(context)) {
                cancel(taskId)
            }
            return
        }

        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        
        val settingsManager = SettingsManager(context)
        val notificationsEnabled = runBlocking {
            settingsManager.notificationsEnabledFlow.first()
        }

        if (notificationsEnabled) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                
                val openAppIntent = Intent(Intent.ACTION_VIEW, "tasktracker://task/$taskId".toUri()).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingContentIntent = android.app.PendingIntent.getActivity(
                    context,
                    taskId,
                    openAppIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val completedIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
                    this.action = "MARK_COMPLETED"
                    putExtra("TASK_ID", taskId)
                }
                val pendingCompletedIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    taskId * 10,
                    completedIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, "task_alerts")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Task Due")
                    .setContentText(taskTitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingContentIntent)
                    .addAction(0, "Mark as Completed", pendingCompletedIntent)
                    .addAction(0, "Reschedule", pendingContentIntent)

                with(NotificationManagerCompat.from(context)) {
                    notify(taskId, builder.build())
                }
            }
        }
    }
}
