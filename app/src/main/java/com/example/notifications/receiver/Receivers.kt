package com.example.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.dao.ReminderDao
import com.example.data.dao.TaskDao
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.ReminderType
import com.example.domain.repository.TaskRepository
import com.example.notifications.manager.NotificationHelper
import com.example.utils.DateTimeUtils
import com.example.workers.ReminderRescheduleWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderDao: ReminderDao
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (reminderId <= 0L || taskId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = reminderDao.getReminderById(reminderId) ?: return@launch
                if (!reminder.isEnabled || reminder.isFired) return@launch

                val details = taskDao.getTaskWithDetailsById(taskId) ?: return@launch
                if (details.task.isCompleted) return@launch

                val type = ReminderType.fromString(reminder.reminderType)
                if (type == ReminderType.OVERDUE && details.task.overdueNotified) {
                    return@launch
                }

                reminderDao.markFired(reminderId)
                if (type == ReminderType.OVERDUE) {
                    taskDao.setOverdueNotified(taskId, notified = true, updatedAt = System.currentTimeMillis())
                }

                val prefs = preferencesRepository.preferencesFlow.first()
                notificationHelper.showTaskReminderNotification(
                    taskId = details.task.id,
                    taskTitle = details.task.title,
                    scheduledTimeText = DateTimeUtils.formatTimeRange(details.task.startTime, details.task.dueTime),
                    offsetMinutes = reminder.offsetMinutes,
                    reminderType = type,
                    notificationId = reminder.notificationId,
                    soundEnabled = prefs.notificationSoundEnabled,
                    vibrationEnabled = prefs.notificationVibrationEnabled
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        if (taskId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    NotificationHelper.ACTION_MARK_DONE -> {
                        if (notificationId != -1) {
                            notificationHelper.cancelNotification(notificationId)
                        }
                        taskRepository.toggleTaskCompleted(taskId, completed = true)
                    }
                    NotificationHelper.ACTION_SNOOZE -> {
                        val snoozeMinutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 10)
                        taskRepository.snoozeTaskReminder(
                            taskId = taskId,
                            snoozeMinutes = snoozeMinutes,
                            dismissedNotificationId = notificationId
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

@AndroidEntryPoint
class BootAndTimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED

        if (!relevant) return

        val request = OneTimeWorkRequestBuilder<ReminderRescheduleWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "justdo_immediate_reschedule",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
