package com.example.presentation.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMono
import com.example.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    initialTaskId: Long?,
    state: FocusUiState,
    onSelectTask: (Long?) -> Unit,
    onSelectPhase: (FocusPhase) -> Unit,
    onToggleStartPause: () -> Unit,
    onReset: () -> Unit,
    onCompleteEarly: () -> Unit,
    onClearNotice: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(initialTaskId) {
        if (initialTaskId != null) {
            onSelectTask(initialTaskId)
        }
    }

    LaunchedEffect(state.sessionSavedNotice) {
        val msg = state.sessionSavedNotice
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearNotice()
        }
    }

    val progress = if (state.totalSeconds > 0) {
        state.remainingSeconds.toFloat() / state.totalSeconds.toFloat()
    } else 1f
    val animatedSweep by animateFloatAsState(targetValue = progress * 360f, label = "pomodoroArc")
    val ringColor = if (state.phase == FocusPhase.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Focus Mode & Pomodoro") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("focus_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    FocusPhase.entries.forEachIndexed { idx, phase ->
                        SegmentedButton(
                            selected = state.phase == phase,
                            onClick = { onSelectPhase(phase) },
                            shape = SegmentedButtonDefaults.itemShape(idx, FocusPhase.entries.size),
                            modifier = Modifier.testTag("focus_phase_${phase.name.lowercase()}")
                        ) {
                            Text(phase.label, maxLines = 1)
                        }
                    }
                }

                // Circular Countdown Ring
                Box(
                    modifier = Modifier.size(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 16.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.phase.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ringColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = DateTimeUtils.formatCountdown(state.remainingSeconds),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.completedFocusCycles} cycles completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("focus_reset_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }

                    Button(
                        onClick = onToggleStartPause,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("focus_start_pause_button")
                    ) {
                        Icon(
                            if (state.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (state.isRunning) "Pause" else "Start")
                    }
                }

                if (state.remainingSeconds < state.totalSeconds) {
                    FilledTonalButton(
                        onClick = onCompleteEarly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("focus_save_session_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish & Log Session")
                    }
                }

                // Focus Task Picker
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Focus on a Task", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Link this Pomodoro session to one of your pending tasks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedTaskId == null,
                                    onClick = { onSelectTask(null) },
                                    label = { Text("Free Focus") }
                                )
                            }
                            items(state.pendingTasks, key = { it.id }) { task ->
                                FilterChip(
                                    selected = state.selectedTaskId == task.id,
                                    onClick = { onSelectTask(task.id) },
                                    label = {
                                        Text(
                                            text = task.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
