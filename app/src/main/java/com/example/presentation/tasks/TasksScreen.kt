package com.example.presentation.tasks

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.domain.model.StatusFilter
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskSort
import com.example.presentation.components.EmptyStateView
import com.example.presentation.components.TaskCard
import com.example.utils.DateTimeUtils
import java.time.LocalDate

@Composable
fun TasksScreen(
    state: TasksUiState,
    onQueryChange: (String) -> Unit,
    onStatusChange: (StatusFilter) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onPriorityChange: (TaskPriority?) -> Unit,
    onDateChange: (Long?) -> Unit,
    onSortChange: (TaskSort) -> Unit,
    onClearFilters: () -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenNewTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("tasks_add_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Tasks (${state.tasks.size})",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Box {
                        TextButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("tasks_sort_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sort: ${state.filter.sort.label}")
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            TaskSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    onClick = {
                                        onSortChange(sort)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.filter.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search tasks, notes, or categories...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.filter.query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tasks_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status & Date chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(StatusFilter.entries) { status ->
                        FilterChip(
                            selected = state.filter.status == status,
                            onClick = { onStatusChange(status) },
                            label = { Text(status.label) },
                            modifier = Modifier.testTag("status_filter_${status.name.lowercase()}")
                        )
                    }
                    item {
                        val dateSel = state.filter.dateEpochDay
                        FilterChip(
                            selected = dateSel != null,
                            onClick = {
                                if (dateSel != null) {
                                    onDateChange(null)
                                } else {
                                    val today = LocalDate.now()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            onDateChange(LocalDate.of(y, m + 1, d).toEpochDay())
                                        },
                                        today.year,
                                        today.monthValue - 1,
                                        today.dayOfMonth
                                    ).show()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            label = {
                                Text(if (dateSel != null) "${DateTimeUtils.formatShortDate(dateSel)} ×" else "By Date")
                            }
                        )
                    }
                }

                // Category & Priority filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories, key = { it.id }) { cat ->
                        val selected = state.filter.categoryId == cat.id
                        FilterChip(
                            selected = selected,
                            onClick = { onCategoryChange(if (selected) null else cat.id) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.color))
                                )
                            },
                            label = { Text(cat.name) }
                        )
                    }
                    items(TaskPriority.entries) { prio ->
                        val selected = state.filter.priority == prio
                        FilterChip(
                            selected = selected,
                            onClick = { onPriorityChange(if (selected) null else prio) },
                            label = { Text(prio.label) }
                        )
                    }
                }

                if (state.filter.query.isNotEmpty() ||
                    state.filter.status != StatusFilter.ALL ||
                    state.filter.categoryId != null ||
                    state.filter.priority != null ||
                    state.filter.dateEpochDay != null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onClearFilters) {
                            Text("Reset all filters")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (state.tasks.isEmpty()) {
                    EmptyStateView(
                        title = if (state.totalTaskCount == 0) "No tasks yet" else "No matching tasks",
                        subtitle = if (state.totalTaskCount == 0) {
                            "Create tasks with custom priorities, subtasks, and offline reminders."
                        } else {
                            "Try adjusting your search keywords or active filters."
                        },
                        actionLabel = if (state.totalTaskCount == 0) "Add Task" else "Reset Filters",
                        onAction = if (state.totalTaskCount == 0) onOpenNewTask else onClearFilters
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { onToggleComplete(task.id, it) },
                                onClick = { onOpenTask(task.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
