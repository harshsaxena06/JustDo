package com.example.presentation.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.presentation.components.EmptyStateView
import com.example.presentation.components.TaskCard
import com.example.ui.theme.JetBrainsMono
import com.example.utils.DateTimeUtils

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onQuickAdd: (String) -> Unit,
    onOpenNewTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onToggleTaskCompleted: (Long, Boolean) -> Unit,
    onRescheduleToToday: (Long) -> Unit,
    onNextQuote: () -> Unit,
    onOpenFocusTimer: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var quickTitle by remember { mutableStateOf("") }
    val animatedProgress by animateFloatAsState(
        targetValue = state.completionFraction,
        animationSpec = tween(durationMillis = 500),
        label = "dailyProgress"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("dashboard_add_task_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add new task")
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Greeting & Live Clock Banner
                item {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_dashboard_banner),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xD91E1B4B),
                                                Color(0xB34338CA),
                                                Color(0x997C3AED)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = state.greeting,
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            text = state.fullDate,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.16f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LocalFireDepartment,
                                                contentDescription = "Streak",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${state.streakDays}d streak",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = state.liveClock,
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontFamily = JetBrainsMono,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                    FilledTonalButton(
                                        onClick = onOpenFocusTimer,
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.White.copy(alpha = 0.22f),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("dashboard_focus_button")
                                    ) {
                                        Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pomodoro")
                                    }
                                }
                            }
                        }
                    }
                }

                // Offline Permission / Exact Alarm Notice if needed
                if (!state.hasNotificationPermission || !state.canScheduleExactAlarms) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (!state.hasNotificationPermission) {
                                            "Enable notifications for local alarms"
                                        } else {
                                            "Allow Exact Alarms for on-the-minute alerts"
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "All reminders run 100% offline on your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (!state.hasNotificationPermission) {
                                            onRequestNotificationPermission()
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    modifier = Modifier.testTag("enable_alarms_button")
                                ) {
                                    Text("Enable")
                                }
                            }
                        }
                    }
                }

                // Daily Summary & Progress Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Daily Summary", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Daily Goal: ${state.completedTodayCount} / ${state.dailyGoal} completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${state.completionPercent}%",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryMetricPill(
                                    label = "Today",
                                    value = state.todayTasks.size.toString(),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetricPill(
                                    label = "Done",
                                    value = state.completedTodayCount.toString(),
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetricPill(
                                    label = "Pending",
                                    value = state.pendingTodayCount.toString(),
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                SummaryMetricPill(
                                    label = "Overdue",
                                    value = state.overdueTasks.size.toString(),
                                    containerColor = if (state.overdueTasks.isNotEmpty()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (state.overdueTasks.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Quick Add Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickTitle,
                            onValueChange = { quickTitle = it },
                            placeholder = { Text("Quick add a task for today...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_add_input")
                        )
                        Button(
                            onClick = {
                                if (quickTitle.isNotBlank()) {
                                    onQuickAdd(quickTitle)
                                    quickTitle = ""
                                }
                            },
                            modifier = Modifier
                                .height(54.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("quick_add_submit_button")
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "Quick add task")
                        }
                    }
                }

                // Motivational Quote Card
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "\"${state.quote.text}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "— ${state.quote.author}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onNextQuote,
                                modifier = Modifier.testTag("refresh_quote_button")
                            ) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Next quote", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Upcoming Reminders Strip
                if (state.upcomingReminders.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Upcoming Local Alarms", style = MaterialTheme.typography.titleMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(state.upcomingReminders, key = { it.reminder.id }) { item ->
                                    Card(
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .width(210.dp)
                                            .clickable { onOpenTask(item.reminder.taskId) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.Alarm,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = DateTimeUtils.formatEpochMillis(item.reminder.reminderDateTime),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.taskTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.reminder.reminderType.label,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Overdue Section
                if (state.overdueTasks.isNotEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Overdue (${state.overdueTasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    items(state.overdueTasks, key = { "overdue_${it.id}" }) { task ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TaskCard(
                                task = task,
                                onToggleComplete = { onToggleTaskCompleted(task.id, it) },
                                onClick = { onOpenTask(task.id) }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { onRescheduleToToday(task.id) }) {
                                    Text("Move to Today")
                                }
                            }
                        }
                    }
                }

                // Today's Tasks
                item {
                    Text(
                        text = "Today's Tasks (${state.todayTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (state.todayTasks.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Your day is wide open",
                            subtitle = "Tap Quick Add above or the + button to schedule your first offline task & reminder.",
                            actionLabel = "Create Task",
                            onAction = onOpenNewTask
                        )
                    }
                } else {
                    items(state.todayTasks, key = { "today_${it.id}" }) { task ->
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
private fun SummaryMetricPill(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
    }
}
