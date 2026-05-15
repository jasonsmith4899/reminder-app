package com.mason.reminder.ui.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.repository.CategoryRepository
import com.mason.reminder.data.repository.TaskRepository
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.daysUntil
import com.mason.reminder.util.toEpochMillis
import com.mason.reminder.util.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 任务 + 紧急度 + 倒计时天数，用于 TaskListScreen 列表渲染。
 */
data class TaskWithUrgency(
    val task: Task,
    val urgency: UrgencyState,
    val daysLeft: Long
)

enum class SortMode { BY_URGENCY, BY_DUE_DATE }

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: -1L

    // ── 排序 ──────────────────────────────────────

    private val _sortMode = MutableStateFlow(SortMode.BY_URGENCY)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    fun toggleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.BY_URGENCY -> SortMode.BY_DUE_DATE
            SortMode.BY_DUE_DATE -> SortMode.BY_URGENCY
        }
    }

    // ── 批量选择 ──────────────────────────────────

    private val _selectedTasks = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTasks: StateFlow<Set<Long>> = _selectedTasks.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedTasks.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedCount: StateFlow<Int> = _selectedTasks.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleTaskSelection(taskId: Long) {
        val current = _selectedTasks.value
        _selectedTasks.value = if (taskId in current) current - taskId else current + taskId
    }

    fun selectAll() {
        _selectedTasks.value = tasksWithUrgency.value.map { it.task.id }.toSet()
    }

    fun clearSelection() {
        _selectedTasks.value = emptySet()
    }

    fun deleteSelectedTasks() {
        viewModelScope.launch {
            val selected = _selectedTasks.value
            tasksWithUrgency.value
                .filter { it.task.id in selected }
                .forEach { taskRepository.delete(it.task) }
            _selectedTasks.value = emptySet()
        }
    }

    fun completeSelectedTasks() {
        viewModelScope.launch {
            _selectedTasks.value.forEach { taskRepository.markCompleted(it) }
            _selectedTasks.value = emptySet()
        }
    }

    // ── 列表数据 ──────────────────────────────────

    val tasksWithUrgency: StateFlow<List<TaskWithUrgency>> = combine(
        taskRepository.getByCategory(categoryId),
        _sortMode
    ) { tasks, mode ->
        val now = LocalDate.now()
        val withUrgency = tasks.map { task ->
            val urgency = UrgencyCalculator.calculate(task, now)
            val daysLeft = task.nextDueDate.toLocalDate().daysUntil(now)
            TaskWithUrgency(task, urgency, daysLeft)
        }
        when (mode) {
            SortMode.BY_URGENCY -> withUrgency.sortedWith(
                compareByDescending<TaskWithUrgency> { it.urgency.ordinal }
                    .thenBy { it.task.nextDueDate }
            )
            SortMode.BY_DUE_DATE -> withUrgency.sortedBy { it.task.nextDueDate }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val category: StateFlow<Category?> = categoryRepository.getById(categoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── 单项操作 ──────────────────────────────────

    fun deleteTask(task: Task) {
        viewModelScope.launch { taskRepository.delete(task) }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch { taskRepository.markCompleted(taskId) }
    }

    fun insertTask(
        title: String,
        reminderType: ReminderType,
        reminderLevel: ReminderLevel,
        dueDate: LocalDate?,
        intervalValue: Int?,
        intervalUnit: IntervalUnit?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextDue = when (reminderType) {
                ReminderType.ONCE -> dueDate?.toEpochMillis() ?: now
                ReminderType.RECURRING -> {
                    val start = dueDate ?: LocalDate.now()
                    val nextDate = when (intervalUnit) {
                        IntervalUnit.DAY   -> start.plusDays(intervalValue?.toLong() ?: 1)
                        IntervalUnit.WEEK  -> start.plusWeeks(intervalValue?.toLong() ?: 1)
                        IntervalUnit.MONTH -> start.plusMonths(intervalValue?.toLong() ?: 1)
                        null               -> start
                    }
                    nextDate.toEpochMillis()
                }
            }
            taskRepository.insert(
                Task(
                    id = 0,
                    title = title,
                    description = "",
                    categoryId = categoryId,
                    reminderType = reminderType,
                    reminderLevel = reminderLevel,
                    customAdvanceDays = null,
                    customNotifyFreq = null,
                    intervalValue = if (reminderType == ReminderType.RECURRING) intervalValue else null,
                    intervalUnit = if (reminderType == ReminderType.RECURRING) intervalUnit else null,
                    startDate = dueDate?.toEpochMillis(),
                    dueDate = if (reminderType == ReminderType.ONCE) dueDate?.toEpochMillis() else null,
                    nextDueDate = nextDue,
                    lastCompletedAt = null,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}