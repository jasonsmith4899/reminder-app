package com.mason.reminder.ui.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.model.CustomNotifyFreq
import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.toLocalDate
import com.mason.reminder.data.repository.CategoryRepository
import com.mason.reminder.data.repository.TaskRepository
import com.mason.reminder.util.toEpochMillis
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TaskDetailUiState(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val categoryId: Long = 0,
    val reminderType: ReminderType = ReminderType.ONCE,
    val reminderLevel: ReminderLevel = ReminderLevel.MEDIUM,
    val customAdvanceDays: Int = 3,
    val customNotifyFreq: CustomNotifyFreq = CustomNotifyFreq.DAILY,
    val intervalValue: Int = 1,
    val intervalUnit: IntervalUnit = IntervalUnit.DAY,
    val startDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val taskId: Long = savedStateHandle.get<String>("taskId")?.toLongOrNull() ?: 0L

    private val _uiState = MutableStateFlow(TaskDetailUiState(isLoading = taskId != 0L))
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        if (taskId != 0L) loadTask()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getAllSorted().collect { categories ->
                _uiState.update { state ->
                    val autoId = if (state.categoryId == 0L && categories.isNotEmpty()) {
                        categories.first().id
                    } else {
                        state.categoryId
                    }
                    state.copy(categories = categories, categoryId = autoId)
                }
            }
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            val task = taskRepository.getByIdOnce(taskId)
            if (task != null) {
                _uiState.update { state ->
                    state.copy(
                        id = task.id,
                        title = task.title,
                        description = task.description,
                        categoryId = task.categoryId,
                        reminderType = task.reminderType,
                        reminderLevel = task.reminderLevel,
                        customAdvanceDays = task.customAdvanceDays ?: 3,
                        customNotifyFreq = task.customNotifyFreq ?: CustomNotifyFreq.DAILY,
                        intervalValue = task.intervalValue ?: 1,
                        intervalUnit = task.intervalUnit ?: IntervalUnit.DAY,
                        startDate = task.startDate?.toLocalDate(),
                        dueDate = task.dueDate?.toLocalDate(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "任务不存在") }
            }
        }
    }

    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateDescription(desc: String) = _uiState.update { it.copy(description = desc) }
    fun updateCategoryId(id: Long) = _uiState.update { it.copy(categoryId = id) }

    fun updateReminderType(type: ReminderType) = _uiState.update {
        when (type) {
            ReminderType.ONCE -> it.copy(
                reminderType = type,
                startDate = null,
                dueDate = it.dueDate ?: LocalDate.now().plusDays(7)
            )
            ReminderType.RECURRING -> it.copy(
                reminderType = type,
                dueDate = null,
                startDate = it.startDate ?: LocalDate.now()
            )
        }
    }

    fun updateReminderLevel(level: ReminderLevel) = _uiState.update { it.copy(reminderLevel = level) }
    fun updateCustomAdvanceDays(days: Int) = _uiState.update { it.copy(customAdvanceDays = days) }
    fun updateCustomNotifyFreq(freq: CustomNotifyFreq) = _uiState.update { it.copy(customNotifyFreq = freq) }
    fun updateIntervalValue(value: Int) = _uiState.update { it.copy(intervalValue = value) }
    fun updateIntervalUnit(unit: IntervalUnit) = _uiState.update { it.copy(intervalUnit = unit) }
    fun updateStartDate(date: LocalDate?) = _uiState.update { it.copy(startDate = date) }
    fun updateDueDate(date: LocalDate?) = _uiState.update { it.copy(dueDate = date) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "标题不能为空") }
            return
        }

        val nextDueDate = calculateNextDueDate(state)
        if (nextDueDate == null) {
            val errMsg = if (state.reminderType == ReminderType.ONCE) "请选择到期日期" else "请选择起始日期"
            _uiState.update { it.copy(error = errMsg) }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = if (state.id != 0L) taskRepository.getByIdOnce(state.id) else null

            val task = Task(
                id = state.id,
                title = state.title.trim(),
                description = state.description.trim(),
                categoryId = state.categoryId,
                reminderType = state.reminderType,
                reminderLevel = state.reminderLevel,
                customAdvanceDays = if (state.reminderLevel == ReminderLevel.CUSTOM) state.customAdvanceDays else null,
                customNotifyFreq = if (state.reminderLevel == ReminderLevel.CUSTOM) state.customNotifyFreq else null,
                intervalValue = if (state.reminderType == ReminderType.RECURRING) state.intervalValue else null,
                intervalUnit = if (state.reminderType == ReminderType.RECURRING) state.intervalUnit else null,
                startDate = if (state.reminderType == ReminderType.RECURRING) state.startDate?.toEpochMillis() else null,
                dueDate = if (state.reminderType == ReminderType.ONCE) state.dueDate?.toEpochMillis() else null,
                nextDueDate = nextDueDate,
                lastCompletedAt = existing?.lastCompletedAt,
                isActive = true,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )

            if (state.id == 0L) {
                taskRepository.insert(task)
            } else {
                taskRepository.update(task)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun calculateNextDueDate(state: TaskDetailUiState): Long? {
        return when (state.reminderType) {
            ReminderType.ONCE -> state.dueDate?.toEpochMillis()
            ReminderType.RECURRING -> {
                val start = state.startDate ?: return null
                when (state.intervalUnit) {
                    IntervalUnit.DAY -> start.plusDays(state.intervalValue.toLong())
                    IntervalUnit.WEEK -> start.plusWeeks(state.intervalValue.toLong())
                    IntervalUnit.MONTH -> start.plusMonths(state.intervalValue.toLong())
                }.toEpochMillis()
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}