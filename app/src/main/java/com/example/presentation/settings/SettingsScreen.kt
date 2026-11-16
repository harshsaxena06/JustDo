package com.example.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.presentation.tasks.QuickCategoryDialog
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.ThemeMode
import com.example.utils.RestoreMode
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onDefaultReminderOffsetChange: (Int) -> Unit,
    onPomodoroDurationsChange: (Int, Int) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onVibrationEnabledChange: (Boolean) -> Unit,
    onCreateCategory: (String, Int) -> Unit,
    onDeleteCustomCategory: (Long) -> Unit,
    onExportJson: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onRestoreJson: (Uri, RestoreMode) -> Unit,
    onClearAllLocalData: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onClearStatusMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var clearConfirmStep by remember { mutableStateOf(0) }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExportJson) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportCsv) }

    val restoreJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearStatusMessage()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
            }

            // Appearance
            item {
                SettingsSectionCard(title = "Appearance") {
                    Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = state.preferences.themeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }
                    }
                }
            }

            // Daily Goals & Pomodoro
            item {
                SettingsSectionCard(title = "Goals & Focus Timer") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Task Target", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${state.preferences.dailyGoal} tasks",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = state.preferences.dailyGoal.toFloat(),
                        onValueChange = { onDailyGoalChange(it.roundToInt()) },
                        valueRange = 1f..20f,
                        steps = 18
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pomodoro Preset", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(25 to 5, 45 to 10, 50 to 10, 15 to 3).forEach { (focus, brk) ->
                            FilterChip(
                                selected = state.preferences.pomodoroFocusMinutes == focus && state.preferences.pomodoroBreakMinutes == brk,
                                onClick = { onPomodoroDurationsChange(focus, brk) },
                                label = { Text("${focus}m / ${brk}m") }
                            )
                        }
                    }
                }
            }

            // Offline Alarms & Notifications
            item {
                SettingsSectionCard(title = "Offline Alarms & Notifications") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Notification Permission", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (state.hasNotificationPermission) "Granted" else "Required for reminders on Android 13+",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.hasNotificationPermission) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                            )
                        }
                        if (!state.hasNotificationPermission) {
                            TextButton(onClick = onRequestNotificationPermission) { Text("Grant") }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exact Alarm Scheduling", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = if (state.canScheduleExactAlarms) "Exact alarms enabled" else "Standard fallback active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            TextButton(onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }) {
                                Text("Configure")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Default Reminder Offset", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "At time", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 60 to "1h").forEach { (mins, label) ->
                            FilterChip(
                                selected = state.preferences.defaultReminderOffsetMinutes == mins,
                                onClick = { onDefaultReminderOffsetChange(mins) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification Sound", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = state.preferences.notificationSoundEnabled, onCheckedChange = onSoundEnabledChange)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Notification Vibration", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = state.preferences.notificationVibrationEnabled, onCheckedChange = onVibrationEnabledChange)
                    }
                }
            }

            // Custom Categories Management
            item {
                SettingsSectionCard(title = "Categories") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Category, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${state.categories.size} categories", style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(
                            onClick = { showNewCategoryDialog = true },
                            modifier = Modifier.testTag("settings_add_category_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Category")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.categories.forEach { cat ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = if (cat.isDefault) 10.dp else 4.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(cat.color))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(cat.name, style = MaterialTheme.typography.labelLarge)
                                    if (!cat.isDefault) {
                                        IconButton(
                                            onClick = { onDeleteCustomCategory(cat.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = "Delete custom category",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Local Backup, Export & Restore
            item {
                SettingsSectionCard(title = "Local Backup & Export") {
                    Text(
                        text = "Export or restore all data locally using Android's file picker—zero internet or storage permissions required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { exportJsonLauncher.launch("justdo_backup_${System.currentTimeMillis()}.json") },
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Outlined.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Full Backup (JSON)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { exportCsvLauncher.launch("justdo_tasks_${System.currentTimeMillis()}.csv") },
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Tasks Spreadsheet (CSV)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { restoreJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        enabled = !state.isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_json_button")
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore from JSON Backup")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { clearConfirmStep = 1 },
                        enabled = !state.isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_all_data_button")
                    ) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Local Data")
                    }
                }
            }

            // Offline Security Guarantee
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("100% Offline & Private", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "JustDo has no internet permission, no cloud sync, and no tracking. Everything stays safely on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showNewCategoryDialog) {
        QuickCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onCreate = { name, color ->
                onCreateCategory(name, color)
                showNewCategoryDialog = false
            }
        )
    }

    if (pendingRestoreUri != null) {
        val uri = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore Backup") },
            text = {
                Text("Choose whether to merge backed-up tasks with your current tasks, or replace all current data.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    onRestoreJson(uri, RestoreMode.MERGE)
                }) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
                    TextButton(onClick = {
                        pendingRestoreUri = null
                        onRestoreJson(uri, RestoreMode.REPLACE)
                    }) {
                        Text("Replace All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    if (clearConfirmStep == 1) {
        AlertDialog(
            onDismissRequest = { clearConfirmStep = 0 },
            title = { Text("Clear All Local Data?") },
            text = { Text("This will delete all tasks, subtasks, reminders, focus sessions, and custom categories.") },
            confirmButton = {
                TextButton(onClick = { clearConfirmStep = 2 }) {
                    Text("Continue", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmStep = 0 }) { Text("Cancel") }
            }
        )
    } else if (clearConfirmStep == 2) {
        AlertDialog(
            onDismissRequest = { clearConfirmStep = 0 },
            title = { Text("Final Confirmation") },
            text = { Text("Are you sure? Export a JSON backup first if you want to keep a copy. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    clearConfirmStep = 0
                    onClearAllLocalData()
                }) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirmStep = 0 }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
