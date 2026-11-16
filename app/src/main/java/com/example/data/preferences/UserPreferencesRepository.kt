package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "justdo_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dailyGoal: Int = 5,
    val defaultReminderOffsetMinutes: Int = 15,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5,
    val notificationSoundEnabled: Boolean = true,
    val notificationVibrationEnabled: Boolean = true
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val DEFAULT_REMINDER_OFFSET = intPreferencesKey("default_reminder_offset")
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_BREAK_MINUTES = intPreferencesKey("pomodoro_break_minutes")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                ThemeMode.entries.firstOrNull { it.name == name }
            } ?: ThemeMode.SYSTEM,
            dailyGoal = (prefs[Keys.DAILY_GOAL] ?: 5).coerceIn(1, 30),
            defaultReminderOffsetMinutes = prefs[Keys.DEFAULT_REMINDER_OFFSET] ?: 15,
            pomodoroFocusMinutes = (prefs[Keys.POMODORO_FOCUS_MINUTES] ?: 25).coerceIn(5, 90),
            pomodoroBreakMinutes = (prefs[Keys.POMODORO_BREAK_MINUTES] ?: 5).coerceIn(1, 30),
            notificationSoundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            notificationVibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(1, 30) }
    }

    suspend fun setDefaultReminderOffset(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_REMINDER_OFFSET] = minutes }
    }

    suspend fun setPomodoroDurations(focusMinutes: Int, breakMinutes: Int) {
        context.dataStore.edit {
            it[Keys.POMODORO_FOCUS_MINUTES] = focusMinutes.coerceIn(5, 90)
            it[Keys.POMODORO_BREAK_MINUTES] = breakMinutes.coerceIn(1, 30)
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun resetDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
