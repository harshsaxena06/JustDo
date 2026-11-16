package com.example.presentation.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.database.AppDatabase
import com.example.domain.model.CategoryItem
import com.example.domain.model.ReminderOption
import com.example.domain.model.ReminderType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.utils.DateTimeUtils
import com.example.utils.RecurrenceRule
import com.example.utils.RecurrenceType
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorSheet(
    existingTask: TaskItem? = null,
    initialDateEpochDay: Long = DateTimeUtils.todayEpochDay(),
    defaultReminderOffsetMinutes: Int = 15,
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onCreateQuickCategory: (String, Int) -> Unit = { _, _ -> },
    onSave: (
        taskId: Long?,
        title: String,
        description: String,
        notes: String,
        categoryId: Long,
        priority: TaskPriority,
        scheduledDate: Long,
        startTime: Long?,
        dueTime: Long?,
        isRecurring: Boolean,
        recurrenceRule: RecurrenceRule,
        subtasks: List<Pair<Long, String>>,
        reminderOptions: List<ReminderOption>
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var description by remember { mutableStateOf(existingTask?.description ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var categoryId by remember {
        mutableStateOf(
            existingTask?.categoryId ?: categories.firstOrNull()?.id ?: AppDatabase.DEFAULT_OTHER_CATEGORY_ID
        )
    }
    var priority by remember { mutableStateOf(existingTask?.priority ?: TaskPriority.MEDIUM) }
    var scheduledDate by remember { mutableStateOf(existingTask?.scheduledDate ?: initialDateEpochDay) }
    var startTime by remember { mutableStateOf(existingTask?.startTime) }
    var dueTime by remember { mutableStateOf(existingTask?.dueTime) }

    var recurrenceType by remember {
        mutableStateOf(
            if (existingTask?.isRecurring == true) existingTask.recurrenceRule.type else RecurrenceType.NONE
        )
    }
    var customIntervalDays by remember {
        mutableStateOf((existingTask?.recurrenceRule?.intervalDays ?: 2).toString())
    }
    val customDaysOfWeek = remember {
        mutableStateListOf<Int>().apply {
            addAll(existingTask?.recurrenceRule?.daysOfWeek ?: emptyList())
        }
    }

    val subtasksState = remember {
        mutableStateListOf<Pair<Long, String>>().apply {
            existingTask?.subtasks?.forEach { add(it.id to it.title) }
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    val selectedReminders = remember {
        mutableStateListOf<ReminderOption>().apply {
            if (existingTask != null) {
                existingTask.reminders
                    .filter { it.reminderType != ReminderType.OVERDUE && it.reminderType != ReminderType.SNOOZED }
                    .forEach { add(ReminderOption(it.reminderType, it.offsetMinutes)) }
            } else {
                val defaultType = ReminderType.entries.firstOrNull {
                    it.defaultOffsetMinutes == defaultReminderOffsetMinutes &&
                        it != ReminderType.OVERDUE && it != ReminderType.SNOOZED && it != ReminderType.CUSTOM
                } ?: ReminderType.CUSTOM
                add(ReminderOption(defaultType, defaultReminderOffsetMinutes))
            }
        }
    }

    var showCustomReminderDialog by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presetOffsets = listOf(
        ReminderOption(ReminderType.AT_TIME, 0),
        ReminderOption(ReminderType.BEFORE_5M, 5),
        ReminderOption(ReminderType.BEFORE_10M, 10),
        ReminderOption(ReminderType.BEFORE_15M, 15),
        ReminderOption(ReminderType.BEFORE_30M, 30),
        ReminderOption(ReminderType.BEFORE_60M, 60)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTask == null) "Create Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close editor")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = { Text("Task title *") },
                placeholder = { Text("e.g., Complete Java DSA Practice") },
                singleLine = true,
                isError = validationError != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("What needs to get done?") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_description_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Additional details, links, or checklist thoughts") },
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule Date & Start / End Time
            Text("Schedule", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val d = DateTimeUtils.epochDayToLocalDate(scheduledDate)
                        DatePickerDialog(
                            context,
                            { _, y, m, day ->
                                scheduledDate = LocalDate.of(y, m + 1, day).toEpochDay()
                            },
                            d.year,
                            d.monthValue - 1,
                            d.dayOfMonth
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("task_date_picker_button")
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(DateTimeUtils.formatShortDate(scheduledDate), maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        val init = startTime?.let { DateTimeUtils.minutesToLocalTime(it) } ?: LocalTime.now().plusHours(1)
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                val mins = (h * 60 + min).toLong()
                                startTime = mins
                                if (dueTime != null && dueTime!! < mins) {
                                    dueTime = (mins + 60L).coerceAtMost(1439L)
                                }
                            },
                            init.hour,
                            init.minute,
                            false
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("task_start_time_button")
                ) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(startTime?.let { DateTimeUtils.formatMinutes(it) } ?: "Start time", maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val init = dueTime?.let { DateTimeUtils.minutesToLocalTime(it) }
                            ?: startTime?.let { DateTimeUtils.minutesToLocalTime((it + 60).coerceAtMost(1439L)) }
                            ?: LocalTime.now().plusHours(2)
                        TimePickerDialog(
                            context,
                            { _, h, min ->
                                dueTime = (h * 60 + min).toLong()
                            },
                            init.hour,
                            init.minute,
                            false
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("task_due_time_button")
                ) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dueTime?.let { "End: ${DateTimeUtils.formatMinutes(it)}" } ?: "End / Due time", maxLines = 1)
                }

                if (startTime != null || dueTime != null) {
                    TextButton(onClick = {
                        startTime = null
                        dueTime = null
                    }) {
                        Text("Clear time")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority
            Text("Priority", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = p.color.copy(alpha = 0.18f),
                            selectedLabelColor = p.color
                        ),
                        modifier = Modifier.testTag("priority_chip_${p.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = { showNewCategoryDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    val catColor = Color(cat.color)
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                        },
                        label = { Text(cat.name) },
                        modifier = Modifier.testTag("category_chip_${cat.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminders (Multiple + Custom Interval)
            Text("Local Reminders (AlarmManager)", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (startTime == null && dueTime == null) {
                    "Set a start or due time above to arm local reminders."
                } else {
                    "Select one or more reminders before your scheduled time."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetOffsets.forEach { opt ->
                    val isSelected = selectedReminders.any { it.offsetMinutes == opt.offsetMinutes }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedReminders.removeAll { it.offsetMinutes == opt.offsetMinutes }
                            } else {
                                selectedReminders.add(opt)
                            }
                        },
                        label = { Text(opt.displayLabel()) },
                        modifier = Modifier.testTag("reminder_chip_${opt.offsetMinutes}")
                    )
                }

                // Show any custom intervals
                selectedReminders
                    .filter { rem -> presetOffsets.none { it.offsetMinutes == rem.offsetMinutes } }
                    .forEach { custom ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedReminders.remove(custom) },
                            label = { Text("${custom.offsetMinutes}m before ×") }
                        )
                    }

                FilterChip(
                    selected = false,
                    onClick = { showCustomReminderDialog = true },
                    label = { Text("+ Custom") },
                    modifier = Modifier.testTag("reminder_custom_button")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recurrence
            Text("Repeat Schedule", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RecurrenceType.entries.forEach { type ->
                    FilterChip(
                        selected = recurrenceType == type,
                        onClick = { recurrenceType = type },
                        label = { Text(type.label.substringBefore(" (")) },
                        modifier = Modifier.testTag("recurrence_chip_${type.name.lowercase()}")
                    )
                }
            }

            if (recurrenceType == RecurrenceType.CUSTOM) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customIntervalDays,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            customIntervalDays = input
                        }
                    },
                    label = { Text("Repeat every N days (if no weekdays selected)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Or pick specific days of week:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                val dayLabels = listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayLabels.forEach { (isoDay, label) ->
                        val picked = isoDay in customDaysOfWeek
                        FilterChip(
                            selected = picked,
                            onClick = {
                                if (picked) customDaysOfWeek.remove(isoDay) else customDaysOfWeek.add(isoDay)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtasks
            Text("Subtasks", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text("Add a subtask...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("subtask_input")
                )
                IconButton(
                    onClick = {
                        val trimmed = newSubtaskText.trim()
                        if (trimmed.isNotEmpty()) {
                            subtasksState.add(0L to trimmed)
                            newSubtaskText = ""
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_subtask_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add subtask")
                }
            }

            subtasksState.forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pair.second,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { subtasksState.removeAt(index) }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Remove subtask", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty()) {
                        validationError = "Please enter a task title."
                        return@Button
                    }
                    if (startTime != null && dueTime != null && dueTime!! < startTime!!) {
                        validationError = "End time cannot be earlier than start time."
                        return@Button
                    }
                    val rule = RecurrenceRule(
                        type = recurrenceType,
                        intervalDays = (customIntervalDays.toIntOrNull() ?: 1).coerceAtLeast(1),
                        daysOfWeek = customDaysOfWeek.toList()
                    )
                    onSave(
                        existingTask?.id,
                        trimmedTitle,
                        description,
                        notes,
                        categoryId,
                        priority,
                        scheduledDate,
                        startTime,
                        dueTime,
                        recurrenceType != RecurrenceType.NONE,
                        rule,
                        subtasksState.toList(),
                        selectedReminders.toList()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (existingTask == null) "Save Task & Schedule Alarms" else "Update Task")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCustomReminderDialog) {
        var minsInput by remember { mutableStateOf("45") }
        AlertDialog(
            onDismissRequest = { showCustomReminderDialog = false },
            title = { Text("Custom Reminder Interval") },
            text = {
                OutlinedTextField(
                    value = minsInput,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) minsInput = it },
                    label = { Text("Minutes before task") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val mins = minsInput.toIntOrNull()?.coerceIn(1, 1440) ?: 15
                    if (selectedReminders.none { it.offsetMinutes == mins }) {
                        selectedReminders.add(ReminderOption(ReminderType.CUSTOM, mins))
                    }
                    showCustomReminderDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminderDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewCategoryDialog) {
        QuickCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onCreate = { name, color ->
                onCreateQuickCategory(name, color)
                showNewCategoryDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit
) {
    val swatches = listOf(
        0xFF4F46E5.toInt(),
        0xFF7C3AED.toInt(),
        0xFFEC4899.toInt(),
        0xFF0D9488.toInt(),
        0xFF16A34A.toInt(),
        0xFFEA580C.toInt(),
        0xFF2563EB.toInt(),
        0xFFDC2626.toInt()
    )
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(swatches.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Color tag", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    swatches.forEach { swatch ->
                        val selected = selectedColor == swatch
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .clickable { selectedColor = swatch },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = "Selected color", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onCreate(name.trim(), selectedColor)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
