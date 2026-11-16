package com.example.presentation.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.AnalyticsRange
import com.example.domain.model.CategoryDistributionItem
import com.example.domain.model.DailyProductivityPoint
import com.example.ui.theme.JetBrainsMono
import com.example.utils.DateTimeUtils
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onRangeChange: (AnalyticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = state.summary

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Productivity Analytics", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AnalyticsRange.entries.forEachIndexed { idx, range ->
                        SegmentedButton(
                            selected = summary.range == range,
                            onClick = { onRangeChange(range) },
                            shape = SegmentedButtonDefaults.itemShape(idx, AnalyticsRange.entries.size),
                            modifier = Modifier.testTag("analytics_range_${range.name.lowercase()}")
                        ) {
                            Text(range.label)
                        }
                    }
                }
            }

            // KPI Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Completion Rate",
                            value = "${summary.completionPercentage.roundToInt()}%",
                            subtitle = "${summary.completedTasks} of ${summary.totalTasks} tasks",
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Pending / Overdue",
                            value = "${summary.pendingTasks}",
                            subtitle = "${summary.overdueTasks} overdue",
                            accent = if (summary.overdueTasks > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatTile(
                            label = "Current Streak",
                            value = "${summary.currentStreakDays}d",
                            subtitle = "Best: ${summary.bestStreakDays}d",
                            accent = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Focus Logged",
                            value = "${summary.totalFocusMinutes}m",
                            subtitle = "Best day: ${summary.mostProductiveDay}",
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Productivity Bar Chart
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${summary.range.label} Completion Trend",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed vs. scheduled tasks (computed offline)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        ProductivityBarChart(points = summary.chartPoints)
                    }
                }
            }

            // Category Distribution Donut Chart
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(14.dp))
                        if (summary.categoryDistribution.isEmpty()) {
                            Text(
                                text = "Complete or schedule tasks in this range to see category stats.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            CategoryDistributionChart(items = summary.categoryDistribution)
                        }
                    }
                }
            }

            // Highlights Card
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Most Productive Day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(summary.mostProductiveDay, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${summary.bestStreakDays}d best", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Recent Focus Sessions
            if (state.recentFocusSessions.isNotEmpty()) {
                item {
                    Text("Recent Pomodoro Sessions", style = MaterialTheme.typography.titleMedium)
                }
                items(state.recentFocusSessions, key = { it.session.id }) { row ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.taskTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${row.session.sessionType} • ${DateTimeUtils.formatEpochMillis(row.session.startTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(row.session.durationSeconds / 60).coerceAtLeast(1)}m",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductivityBarChart(points: List<DailyProductivityPoint>) {
    if (points.isEmpty()) return
    val maxTotal = (points.maxOfOrNull { it.totalCount } ?: 1).coerceAtLeast(1)
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = points.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.44f).coerceAtMost(38.dp.toPx())
            val maxBarHeight = size.height - 8.dp.toPx()

            points.forEachIndexed { index, pt ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f

                val totalRatio = pt.totalCount.toFloat() / maxTotal.toFloat()
                val totalHeight = (maxBarHeight * totalRatio).coerceAtLeast(if (pt.totalCount > 0) 10f else 4f)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, size.height - totalHeight),
                    size = Size(barWidth, totalHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                if (pt.completedCount > 0) {
                    val compRatio = pt.completedCount.toFloat() / maxTotal.toFloat()
                    val compHeight = (maxBarHeight * compRatio).coerceAtLeast(10f)
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(left, size.height - compHeight),
                        size = Size(barWidth, compHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${pt.completedCount}/${pt.totalCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryDistributionChart(items: List<CategoryDistributionItem>) {
    val total = items.sumOf { it.totalCount }.coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 22.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                var startAngle = -90f
                items.forEach { item ->
                    val sweep = (item.totalCount.toFloat() / total.toFloat()) * 360f
                    drawArc(
                        color = Color(item.category.color),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono)
                )
                Text("Tasks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(item.category.color))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item.category.name, style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${item.totalCount} (${item.percentage.roundToInt()}%)",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMono),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
