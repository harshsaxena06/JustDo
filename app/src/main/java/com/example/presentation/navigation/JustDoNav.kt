package com.example.presentation.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.presentation.analytics.AnalyticsScreen
import com.example.presentation.analytics.AnalyticsViewModel
import com.example.presentation.calendar.CalendarScreen
import com.example.presentation.calendar.CalendarViewModel
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.dashboard.DashboardViewModel
import com.example.presentation.focus.FocusTimerScreen
import com.example.presentation.focus.FocusViewModel
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.tasks.TaskDetailScreen
import com.example.presentation.tasks.TaskEditorSheet
import com.example.presentation.tasks.TasksScreen
import com.example.presentation.tasks.TasksViewModel
import com.example.utils.DateTimeUtils
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object TasksRoute
@Serializable object CalendarRoute
@Serializable object AnalyticsRoute
@Serializable object SettingsRoute
@Serializable data class TaskDetailRoute(val taskId: Long)
@Serializable data class FocusTimerRoute(val taskId: Long? = null)

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelDestination(TasksRoute, "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "nav_tasks"),
    TopLevelDestination(CalendarRoute, "Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    TopLevelDestination(AnalyticsRoute, "Analytics", Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_analytics"),
    TopLevelDestination(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun JustDoApp(
    widthSizeClass: WindowWidthSizeClass,
    pendingOpenTaskId: Long?,
    onConsumedOpenTaskId: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val tasksViewModel: TasksViewModel = hiltViewModel()
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val focusViewModel: FocusViewModel = hiltViewModel()

    val tasksUiState by tasksViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    var showCreateTaskSheet by remember { mutableStateOf(false) }
    var createTaskDateEpoch by remember { mutableStateOf(DateTimeUtils.todayEpochDay()) }

    LaunchedEffect(pendingOpenTaskId) {
        if (pendingOpenTaskId != null && pendingOpenTaskId > 0L) {
            navController.navigate(TaskDetailRoute(pendingOpenTaskId))
            onConsumedOpenTaskId()
        }
    }

    val isTopLevel = topLevelDestinations.any { dest ->
        currentDestination?.hasRoute(dest.route::class) == true
    }
    val useNavigationRail = widthSizeClass != WindowWidthSizeClass.Compact

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (isTopLevel && !useNavigationRail) {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label, maxLines = 1) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTopLevel && useNavigationRail) {
                NavigationRail {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentDestination?.hasRoute(dest.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.weight(1f)
            ) {
                composable<HomeRoute> {
                    val dashState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                    DashboardScreen(
                        state = dashState,
                        onQuickAdd = dashboardViewModel::quickAddTask,
                        onOpenNewTask = {
                            createTaskDateEpoch = DateTimeUtils.todayEpochDay()
                            showCreateTaskSheet = true
                        },
                        onOpenTask = { id -> navController.navigate(TaskDetailRoute(id)) },
                        onToggleTaskCompleted = dashboardViewModel::toggleTaskCompleted,
                        onRescheduleToToday = dashboardViewModel::rescheduleToToday,
                        onNextQuote = dashboardViewModel::nextQuote,
                        onOpenFocusTimer = { navController.navigate(FocusTimerRoute(null)) },
                        onRequestNotificationPermission = {
                            onRequestNotificationPermission()
                            dashboardViewModel.refreshPermissions()
                        }
                    )
                }

                composable<TasksRoute> {
                    TasksScreen(
                        state = tasksUiState,
                        onQueryChange = tasksViewModel::updateQuery,
                        onStatusChange = tasksViewModel::updateStatus,
                        onCategoryChange = tasksViewModel::updateCategory,
                        onPriorityChange = tasksViewModel::updatePriority,
                        onDateChange = tasksViewModel::updateDateFilter,
                        onSortChange = tasksViewModel::updateSort,
                        onClearFilters = tasksViewModel::clearFilters,
                        onToggleComplete = tasksViewModel::toggleTaskCompleted,
                        onOpenTask = { id -> navController.navigate(TaskDetailRoute(id)) },
                        onOpenNewTask = {
                            createTaskDateEpoch = DateTimeUtils.todayEpochDay()
                            showCreateTaskSheet = true
                        }
                    )
                }

                composable<CalendarRoute> {
                    val calState by calendarViewModel.uiState.collectAsStateWithLifecycle()
                    CalendarScreen(
                        state = calState,
                        onViewModeChange = calendarViewModel::setViewMode,
                        onStatusFilterChange = calendarViewModel::setStatusFilter,
                        onSelectDate = calendarViewModel::selectDate,
                        onPreviousMonth = calendarViewModel::previousMonth,
                        onNextMonth = calendarViewModel::nextMonth,
                        onJumpToToday = calendarViewModel::jumpToToday,
                        onToggleTaskCompleted = calendarViewModel::toggleTaskCompleted,
                        onOpenTask = { id -> navController.navigate(TaskDetailRoute(id)) },
                        onAddTaskForDate = { epochDay ->
                            createTaskDateEpoch = epochDay
                            showCreateTaskSheet = true
                        }
                    )
                }

                composable<AnalyticsRoute> {
                    val analyticsState by analyticsViewModel.uiState.collectAsStateWithLifecycle()
                    AnalyticsScreen(
                        state = analyticsState,
                        onRangeChange = analyticsViewModel::setRange
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        state = settingsUiState,
                        onThemeModeChange = settingsViewModel::setThemeMode,
                        onDailyGoalChange = settingsViewModel::setDailyGoal,
                        onDefaultReminderOffsetChange = settingsViewModel::setDefaultReminderOffset,
                        onPomodoroDurationsChange = settingsViewModel::setPomodoroDurations,
                        onSoundEnabledChange = settingsViewModel::setSoundEnabled,
                        onVibrationEnabledChange = settingsViewModel::setVibrationEnabled,
                        onCreateCategory = settingsViewModel::createCategory,
                        onDeleteCustomCategory = settingsViewModel::deleteCustomCategory,
                        onExportJson = settingsViewModel::exportJson,
                        onExportCsv = settingsViewModel::exportCsv,
                        onRestoreJson = settingsViewModel::restoreJson,
                        onClearAllLocalData = settingsViewModel::clearAllLocalData,
                        onRequestNotificationPermission = {
                            onRequestNotificationPermission()
                            settingsViewModel.refreshPermissions()
                        },
                        onClearStatusMessage = settingsViewModel::clearStatusMessage
                    )
                }

                composable<TaskDetailRoute> { entry ->
                    val args = entry.toRoute<TaskDetailRoute>()
                    TaskDetailScreen(
                        taskId = args.taskId,
                        onBack = { navController.popBackStack() },
                        onStartFocus = { id -> navController.navigate(FocusTimerRoute(id)) },
                        tasksViewModel = tasksViewModel
                    )
                }

                composable<FocusTimerRoute> { entry ->
                    val args = entry.toRoute<FocusTimerRoute>()
                    val focusState by focusViewModel.uiState.collectAsStateWithLifecycle()
                    FocusTimerScreen(
                        initialTaskId = args.taskId,
                        state = focusState,
                        onSelectTask = focusViewModel::selectTask,
                        onSelectPhase = focusViewModel::selectPhase,
                        onToggleStartPause = focusViewModel::toggleStartPause,
                        onReset = focusViewModel::resetTimer,
                        onCompleteEarly = focusViewModel::completeEarlyAndLog,
                        onClearNotice = focusViewModel::clearNotice,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showCreateTaskSheet) {
        TaskEditorSheet(
            existingTask = null,
            initialDateEpochDay = createTaskDateEpoch,
            defaultReminderOffsetMinutes = settingsUiState.preferences.defaultReminderOffsetMinutes,
            categories = tasksUiState.categories,
            onDismiss = { showCreateTaskSheet = false },
            onCreateQuickCategory = settingsViewModel::createCategory,
            onSave = { id, title, desc, notes, catId, prio, date, start, due, rec, rule, subs, rems ->
                tasksViewModel.saveTask(
                    taskId = id,
                    title = title,
                    description = desc,
                    notes = notes,
                    categoryId = catId,
                    priority = prio,
                    scheduledDate = date,
                    startTime = start,
                    dueTime = due,
                    isRecurring = rec,
                    recurrenceRule = rule,
                    subtasks = subs,
                    reminderOptions = rems
                ) {
                    showCreateTaskSheet = false
                }
            }
        )
    }
}
