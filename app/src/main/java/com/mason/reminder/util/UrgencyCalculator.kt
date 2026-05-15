package com.mason.reminder.util

import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import java.time.LocalDate

/**
 * 计算任务紧急度与全局最大紧急度。
 */
object UrgencyCalculator {

    /**
     * 根据 [task] 的 nextDueDate 与当前日期 [now] 计算紧急度。
     *
     * 规则：
     * - daysLeft <= 0 → CRITICAL（今天到期或已过期）
     * - daysLeft <= 1 → URGENT（明天到期）
     * - daysLeft <= 3 → NOTICE（3天内到期）
     * - else           → CALM
     */
    fun calculate(task: Task, now: LocalDate): UrgencyState {
        val dueDate = task.nextDueDate.toLocalDate()
        val daysLeft = dueDate.daysUntil(now)
        return when {
            daysLeft <= 0  -> UrgencyState.CRITICAL
            daysLeft <= 1  -> UrgencyState.URGENT
            daysLeft <= 3  -> UrgencyState.NOTICE
            else           -> UrgencyState.CALM
        }
    }

    /**
     * 计算所有活跃任务中最大的紧急度，用于 Widget / 图标颜色。
     *
     * @param tasks 活跃任务列表
     * @param now   当前日期
     * @return 全局最大 UrgencyState；无活跃任务时返回 CALM
     */
    fun maxUrgency(tasks: List<Task>, now: LocalDate): UrgencyState {
        if (tasks.isEmpty()) return UrgencyState.CALM
        return tasks
            .filter { it.isActive }
            .map { calculate(it, now) }
            .maxByOrNull { it.ordinal }
            ?: UrgencyState.CALM
    }
}