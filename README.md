# JustDo — 100% Offline Smart Daily Task Manager

JustDo is a native Android application built with **Kotlin**, **Jetpack Compose**, and **Material 3** that operates completely offline with zero internet permissions, cloud services, or remote APIs.

## Key Features

- **Complete Task Management (Room SQLite)**: Create, edit, delete, complete, and reopen tasks with descriptions, notes, dates, start & due times, 4 priority levels (`Low`, `Medium`, `High`, `Urgent`), subtasks, and default/custom color-tagged categories.
- **Offline Local Notification Engine (`AlarmManager` + `BroadcastReceiver` + `WorkManager`)**:
  - Exact alarm scheduling (`setExactAndAllowWhileIdle` with graceful `setAndAllowWhileIdle` fallback).
  - Multiple reminders per task (`At time`, `5m`, `10m`, `15m`, `30m`, `60m`, and custom minute intervals).
  - Single overdue alert when a task passes its due time.
  - Inline notification actions: **Mark Done**, **Snooze 5m / 10m / 15m**, and tap-to-open **Task Detail**.
  - Automatic alarm cancellation on delete/complete and rescheduling on task edit.
  - Device reboot (`BOOT_COMPLETED`), package update, clock (`TIME_SET`), and timezone (`TIMEZONE_CHANGED`) recovery plus periodic `WorkManager` reconciliation.
- **Dashboard (Home)**: Personalized greeting, live clock & date, daily goal progress bar, task counts, motivational quotes, quick-add bar, overdue alert list, and upcoming alarms strip.
- **Offline Calendar**: Switchable **Monthly**, **Weekly**, and **Daily** views with category-colored dot indicators, status filtering, and direct task creation per date.
- **Local Search & Filtering**: Instant search across titles, notes, and categories with status, category, priority, date, and sort controls.
- **Recurring Tasks**: Daily, Weekdays, Weekly, Monthly, and Custom (every N days or selected weekdays) schedules with duplicate-free next-occurrence generation.
- **Pomodoro Timer & Focus Mode**: 25m focus / 5m break cycles (customizable in Settings) linked to tasks and persisted to Room `FocusSessionEntity`.
- **Offline Productivity Analytics**: Daily, Weekly, and Monthly Compose `Canvas` bar & donut charts, streaks, most productive weekday, and focus history.
- **Local Backup & Export**: Zero-permission Storage Access Framework (SAF) export to **JSON** (lossless) and **CSV**, JSON restore (Merge or Replace), and double-confirmed local data reset.

## Architecture

- **Presentation**: Jetpack Compose + Material 3 + Type-Safe Navigation Compose + `StateFlow` ViewModels (`@HiltViewModel`).
- **Domain**: Models, Repository interface, and Analytics / Filtering use cases.
- **Data**: Room Database (`TaskEntity`, `ReminderEntity`, `CategoryEntity`, `SubtaskEntity`, `FocusSessionEntity`), DAOs, DataStore Preferences.
- **Background & Alarms**: `AlarmScheduler`, `NotificationHelper`, `ReminderAlarmReceiver`, `NotificationActionReceiver`, `BootAndTimeChangeReceiver`, `ReminderRescheduleWorker` (`@HiltWorker`).
