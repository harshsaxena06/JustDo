package com.example.presentation.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.TaskItem
import com.example.presentation.components.EmptyStateView
import com.example.presentation.components.TaskCard
import com.example.ui.theme.JetBrainsMono
import com.example.utils.DateTimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onViewModeChange: (CalendarViewMode) -> Unit,
    onStatusFilterChange: (CalendarStatusFilter) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    onToggleTaskCompleted: (Long, Boolean) -> Unit,
    onOpenTask: (Long) -> Unit,
    onAddTaskForDate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedEpochDay = DateTimeUtils.localDateToEpochDay(state.selectedDate)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTaskForDate(selectedEpochDay) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("calendar_add_task_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add task on selected date")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header & View Switcher
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calendar", style = MaterialTheme.typography.headlineMedium)
                        TextButton(onClick = onJumpToToday) {
                            Text("Today")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CalendarViewMode.entries.forEachIndexed { idx, mode ->
                            SegmentedButton(
                                selected = state.viewMode == mode,
                                onClick = { onViewModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = CalendarViewMode.entries.size
                                ),
                                modifier = Modifier.testTag("calendar_mode_${mode.name.lowercase()}")
                            ) {
                                Text(mode.label)
                            }
                        }
                    }
                }

                // Monthly or Weekly Picker Card
                if (state.viewMode == CalendarViewMode.MONTHLY) {
                    item {
                        MonthlyCalendarCard(
                            displayedMonth = state.displayedMonth,
                            selectedDate = state.selectedDate,
                            tasksByEpochDay = state.tasksByEpochDay,
                            onSelectDate = onSelectDate,
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth
                        )
                    }
                } else if (state.viewMode == CalendarViewMode.WEEKLY) {
                    item {
                        WeeklyStripCard(
                            selectedDate = state.selectedDate,
                            tasksByEpochDay = state.tasksByEpochDay,
                            onSelectDate = onSelectDate
                        )
                    }
                } else {
                    // Daily header stepper
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onSelectDate(state.selectedDate.minusDays(1)) }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                                }
                                Text(
                                    text = DateTimeUtils.formatFullDate(state.selectedDate),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                IconButton(onClick = { onSelectDate(state.selectedDate.plusDays(1)) }) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                                }
                            }
                        }
                    }
                }

                // Completed / Pending toggle chips + Add Task button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CalendarStatusFilter.entries.forEach { f ->
                                FilterChip(
                                    selected = state.statusFilter == f,
                                    onClick = { onStatusFilterChange(f) },
                                    label = { Text(f.label) }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${DateTimeUtils.formatShortDate(selectedEpochDay)} (${state.selectedDateTasks.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(onClick = { onAddTaskForDate(selectedEpochDay) }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Here")
                        }
                    }
                }

                if (state.selectedDateTasks.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No tasks on ${DateTimeUtils.formatShortDate(selectedEpochDay)}",
                            subtitle = "Schedule a task directly onto this date with custom reminders.",
                            actionLabel = "Add Task",
                            onAction = { onAddTaskForDate(selectedEpochDay) }
                        )
                    }
                } else {
                    items(state.selectedDateTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { onToggleTaskCompleted(task.id, it) },
                            onClick = { onOpenTask(task.id) },
                            showDateBadge = false
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    tasksByEpochDay: Map<Long, List<TaskItem>>,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val today = LocalDate.now()
    val firstDayOfMonth = displayedMonth.atDay(1)
    val startOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = displayedMonth.lengthOfMonth()
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        if (dayNum in 1..daysInMonth) {
                            val date = displayedMonth.atDay(dayNum)
                            val epoch = date.toEpochDay()
                            val dayTasks = tasksByEpochDay[epoch] ?: emptyList()
                            val isSelected = date == selectedDate
                            val isToday = date == today

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = JetBrainsMono,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (dayTasks.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            dayTasks.take(3).forEach { t ->
                                                val dotColor = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    t.category != null -> Color(t.category.color)
                                                    else -> MaterialTheme.colorScheme.secondary
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyStripCard(
    selectedDate: LocalDate,
    tasksByEpochDay: Map<Long, List<TaskItem>>,
    onSelectDate: (LocalDate) -> Unit
) {
    val monday = selectedDate.with(DayOfWeek.MONDAY)
    val today = LocalDate.now()

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onSelectDate(selectedDate.minusWeeks(1)) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week")
                }
                Text(
                    text = "${DateTimeUtils.formatShortDate(monday.toEpochDay())} – ${DateTimeUtils.formatShortDate(monday.plusDays(6).toEpochDay())}",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = { onSelectDate(selectedDate.plusWeeks(1)) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..6).forEach { offset ->
                    val d = monday.plusDays(offset.toLong())
                    val count = tasksByEpochDay[d.toEpochDay()]?.size ?: 0
                    val isSelected = d == selectedDate
                    val isToday = d == today

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                }
                            )
                            .clickable { onSelectDate(d) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = d.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (count > 0) "$count" else "·",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
