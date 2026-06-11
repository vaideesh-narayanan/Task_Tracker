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

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Task Reminder"
        
        val settingsManager = SettingsManager(context)
        val notificationsEnabled = runBlocking {
            settingsManager.notificationsEnabledFlow.first()
        }

        if (notificationsEnabled) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                
                val builder = NotificationCompat.Builder(context, "task_alerts")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Task Due")
                    .setContentText(taskTitle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

                with(NotificationManagerCompat.from(context)) {
                    notify(taskId, builder.build())
                }
            }
        }
    }
}
