package com.mason.reminder.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.repository.CategoryRepository
import com.mason.reminder.data.repository.TaskRepository
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.daysUntil
import com.mason.reminder.util.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * 首页 UI 状态：分类 + 任务统计 + 紧急度。
 */
data class CategoryWithInfo(
    val category: Category,
    val taskCount: Int,
    val mostUrgentTask: Task?,
    val mostUrgentDaysLeft: Long,
    val categoryUrgency: UrgencyState
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    /** 分类列表，每个分类附带任务统计与紧急度。 */
    val categoriesWithInfo: StateFlow<List<CategoryWithInfo>> = combine(
        categoryRepository.getAllSorted(),
        taskRepository.getAllActive()
    ) { categories, tasks ->
        val now = LocalDate.now()
        categories.map { category ->
            val categoryTasks = tasks.filter { it.categoryId == category.id && it.isActive }
            val mostUrgent = categoryTasks.minByOrNull { it.nextDueDate }
            val daysLeft = mostUrgent?.let { task ->
                task.nextDueDate.toLocalDate().daysUntil(now)
            } ?: Long.MAX_VALUE
            val urgency = mostUrgent?.let { task ->
                UrgencyCalculator.calculate(task, now)
            } ?: UrgencyState.CALM
            CategoryWithInfo(
                category = category,
                taskCount = categoryTasks.size,
                mostUrgentTask = mostUrgent,
                mostUrgentDaysLeft = daysLeft,
                categoryUrgency = urgency
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 全局紧急度 — 所有活跃任务中最紧急的状态。 */
    val globalUrgency: StateFlow<UrgencyState> = taskRepository.getAllActive()
        .map { tasks -> UrgencyCalculator.maxUrgency(tasks, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UrgencyState.CALM)

    /** 删除分类及其下所有任务。 */
    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.delete(category) }
    }

    /** 新建分类。 */
    fun insertCategory(name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            categoryRepository.insert(
                Category(
                    id = 0,
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    sortOrder = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** 更新分类名称/图标/颜色。 */
    fun updateCategory(category: Category, name: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            categoryRepository.update(
                category.copy(name = name, iconName = iconName, colorHex = colorHex)
            )
        }
    }
}