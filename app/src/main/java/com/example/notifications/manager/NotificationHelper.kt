package com.example.notifications.manager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.ReminderType
import com.example.notifications.receiver.NotificationActionReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val remindersChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250)
            setSound(alarmSound, audioAttributes)
            enableLights(true)
        }

        val overdueChannel = NotificationChannel(
            CHANNEL_OVERDUE,
            context.getString(R.string.channel_overdue_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_overdue_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 150, 350)
            setSound(alarmSound, audioAttributes)
            enableLights(true)
        }

        val focusChannel = NotificationChannel(
            CHANNEL_FOCUS,
            context.getString(R.string.channel_focus_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_focus_desc)
            enableVibration(true)
        }

        notificationManager.createNotificationChannels(
            listOf(remindersChannel, overdueChannel, focusChannel)
        )
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showTaskReminderNotification(
        taskId: Long,
        taskTitle: String,
        scheduledTimeText: String,
        offsetMinutes: Int,
        reminderType: ReminderType,
        notificationId: Int,
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true
    ) {
        if (!hasNotificationPermission()) return
        createNotificationChannels()

        val isOverdue = reminderType == ReminderType.OVERDUE
        val channelId = if (isOverdue) CHANNEL_OVERDUE else CHANNEL_REMINDERS

        val title = if (isOverdue) "⏰ Overdue Task" else "⏰ Task Reminder"
        val body = when {
            isOverdue -> "$taskTitle was scheduled for $scheduledTimeText. Mark done or snooze?"
            reminderType == ReminderType.SNOOZED -> "Snoozed reminder: $taskTitle ($scheduledTimeText)."
            offsetMinutes > 1 -> "Your $taskTitle session starts in $offsetMinutes minutes. Get ready!"
            offsetMinutes == 1 -> "Your $taskTitle session starts in 1 minute. Get ready!"
            else -> "It's time for $taskTitle ($scheduledTimeText). Let's get started!"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TASK_ID, taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snooze5Pending = buildSnoozePendingIntent(taskId, notificationId, 5, 2)
        val snooze10Pending = buildSnoozePendingIntent(taskId, notificationId, 10, 3)
        val snooze15Pending = buildSnoozePendingIntent(taskId, notificationId, 15, 4)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_mark_done), donePendingIntent)
            .addAction(0, context.getString(R.string.action_snooze_5), snooze5Pending)
            .addAction(0, context.getString(R.string.action_snooze_15), snooze15Pending)
            .addAction(0, context.getString(R.string.action_snooze_10), snooze10Pending)

        if (!soundEnabled) {
            builder.setSilent(true)
        }
        if (vibrationEnabled && soundEnabled) {
            builder.setVibrate(longArrayOf(0, 250, 150, 250))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun showFocusCompleteNotification(isBreak: Boolean, taskTitle: String?) {
        if (!hasNotificationPermission()) return
        createNotificationChannels()

        val title = if (isBreak) "Break Complete" else "Focus Session Complete"
        val body = if (isBreak) {
            "Ready for your next deep focus block?"
        } else {
            if (!taskTitle.isNullOrBlank()) {
                "Great work on $taskTitle! Time for a short break."
            } else {
                "Focus session logged! Take a well-earned break."
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            FOCUS_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(FOCUS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun buildSnoozePendingIntent(
        taskId: Long,
        notificationId: Int,
        minutes: Int,
        salt: Int
    ): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_SNOOZE_MINUTES, minutes)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId * 10 + salt,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "justdo_reminders"
        const val CHANNEL_OVERDUE = "justdo_overdue"
        const val CHANNEL_FOCUS = "justdo_focus"

        const val ACTION_MARK_DONE = "com.example.justdo.ACTION_MARK_DONE"
        const val ACTION_SNOOZE = "com.example.justdo.ACTION_SNOOZE"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_OPEN_TASK_ID = "extra_open_task_id"

        private const val FOCUS_NOTIFICATION_ID = 990011
    }
}
