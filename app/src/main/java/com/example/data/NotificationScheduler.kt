package com.example.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class NotificationScheduler(private val context: Context) {

    fun scheduleTaskAlarm(task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = task.dueDateMillis
        if (triggerTime != null && triggerTime > System.currentTimeMillis() && !task.isCompleted && !task.isDeleted) {
            // Using set to avoid requiring SCHEDULE_EXACT_ALARM on API 31+.
            // This is slightly less precise but much safer without special permission prompts.
            // But we can use setExactAndAllowWhileIdle if we add USE_EXACT_ALARM.
            // Let's use set to save complexity, it should be fine.
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            // Cancel alarm if task is completed or deleted or time is past
            alarmManager.cancel(pendingIntent)
        }
    }
    
    fun cancelTaskAlarm(task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
