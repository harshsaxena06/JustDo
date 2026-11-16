package com.example.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.model.CategoryItem
import com.example.domain.repository.TaskRepository
import com.example.notifications.manager.NotificationHelper
import com.example.notifications.scheduler.AlarmScheduler
import com.example.ui.theme.ThemeMode
import com.example.utils.BackupManager
import com.example.utils.RestoreMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val categories: List<CategoryItem> = emptyList(),
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val isBusy: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val taskRepository: TaskRepository,
    private val backupManager: BackupManager,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val busyState = MutableStateFlow(false)
    private val messageState = MutableStateFlow<String?>(null)
    private val permissionRefresh = MutableStateFlow(0)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferencesFlow,
        taskRepository.observeCategories(),
        busyState,
        messageState,
        permissionRefresh
    ) { prefs, categories, busy, message, _ ->
        SettingsUiState(
            preferences = prefs,
            categories = categories,
            hasNotificationPermission = notificationHelper.hasNotificationPermission(),
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms(),
            isBusy = busy,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            hasNotificationPermission = notificationHelper.hasNotificationPermission(),
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()
        )
    )

    fun refreshPermissions() {
        permissionRefresh.update { it + 1 }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { preferencesRepository.setDailyGoal(goal) }
    }

    fun setDefaultReminderOffset(minutes: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultReminderOffset(minutes) }
    }

    fun setPomodoroDurations(focusMinutes: Int, breakMinutes: Int) {
        viewModelScope.launch { preferencesRepository.setPomodoroDurations(focusMinutes, breakMinutes) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setVibrationEnabled(enabled) }
    }

    fun createCategory(name: String, color: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            taskRepository.createCategory(name, color)
            messageState.value = "Category \"$name\" added"
        }
    }

    fun updateCategory(id: Long, name: String, color: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            taskRepository.updateCategory(id, name, color)
            messageState.value = "Category updated"
        }
    }

    fun deleteCustomCategory(id: Long) {
        viewModelScope.launch {
            taskRepository.deleteCustomCategory(id)
            messageState.value = "Custom category removed (tasks moved to Other)"
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            busyState.value = true
            backupManager.exportJsonToUri(uri)
                .onSuccess { count -> messageState.value = "Exported JSON backup ($count tasks)" }
                .onFailure { err -> messageState.value = "Export failed: ${err.message}" }
            busyState.value = false
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            busyState.value = true
            backupManager.exportCsvToUri(uri)
                .onSuccess { count -> messageState.value = "Exported CSV ($count tasks)" }
                .onFailure { err -> messageState.value = "CSV export failed: ${err.message}" }
            busyState.value = false
        }
    }

    fun restoreJson(uri: Uri, mode: RestoreMode) {
        viewModelScope.launch {
            busyState.value = true
            backupManager.restoreJsonFromUri(uri, mode)
                .onSuccess { res ->
                    messageState.value = "Restored ${res.tasksRestored} tasks & ${res.focusSessionsRestored} focus sessions"
                }
                .onFailure { err ->
                    messageState.value = "Restore failed: Invalid or unreadable backup file (${err.message})"
                }
            busyState.value = false
        }
    }

    fun clearAllLocalData() {
        viewModelScope.launch {
            busyState.value = true
            taskRepository.clearAllLocalData()
            preferencesRepository.resetDefaults()
            busyState.value = false
            messageState.value = "All local data cleared"
        }
    }

    fun clearStatusMessage() {
        messageState.value = null
    }
}
