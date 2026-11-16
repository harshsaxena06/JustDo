package com.example.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CategoryItem
import com.example.domain.model.TaskItem
import com.example.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: TaskItem? = null,
    val categories: List<CategoryItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val taskIdFlow = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TaskDetailUiState> = combine(
        taskIdFlow.flatMapLatest { id ->
            if (id == null) flowOf(null) else taskRepository.observeTask(id)
        },
        taskRepository.observeCategories()
    ) { task, categories ->
        TaskDetailUiState(
            task = task,
            categories = categories,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskDetailUiState()
    )

    fun loadTask(taskId: Long) {
        taskIdFlow.value = taskId
    }

    fun toggleCompleted(completed: Boolean) {
        val id = taskIdFlow.value ?: return
        viewModelScope.launch {
            taskRepository.toggleTaskCompleted(id, completed)
        }
    }

    fun toggleSubtask(subtaskId: Long, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.toggleSubtask(subtaskId, completed)
        }
    }

    fun addSubtask(title: String) {
        val id = taskIdFlow.value ?: return
        viewModelScope.launch {
            taskRepository.addSubtask(id, title)
        }
    }

    fun setReminderEnabled(reminderId: Long, enabled: Boolean) {
        viewModelScope.launch {
            taskRepository.setReminderEnabled(reminderId, enabled)
        }
    }

    fun snooze(minutes: Int) {
        val id = taskIdFlow.value ?: return
        viewModelScope.launch {
            taskRepository.snoozeTaskReminder(id, minutes, -1)
        }
    }

    fun deleteTask(onDeleted: () -> Unit) {
        val id = taskIdFlow.value ?: return
        viewModelScope.launch {
            taskRepository.deleteTask(id)
            onDeleted()
        }
    }
}
