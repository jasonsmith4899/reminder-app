package com.mason.reminder.util

import com.mason.reminder.data.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("ReminderCalculator — 提醒时间计算")
class ReminderCalculatorTest {

    private val dueDate = LocalDate.of(2026, 5, 20)
    private val dueDateMillis = dueDate.toEpochDay() * 86400_000L
    private val defaultNotifyTime = LocalTime.of(9, 0)

    private fun createTask(
        reminderLevel: ReminderLevel,
        reminderType: ReminderType = ReminderType.RECURRING,
        customAdvanceDays: Int? = null,
        intervalValue: Int? = 3,
        intervalUnit: IntervalUnit? = IntervalUnit.DAY,
        startDate: Long? = null,
        dueDateOverride: Long? = dueDateMillis,
        lastCompletedAt: Long? = null
    ): Task = Task(
        id = 1, title = "测试", description = "",
        categoryId = 1, reminderType = reminderType,
        reminderLevel = reminderLevel,
        customAdvanceDays = customAdvanceDays, customNotifyFreq = null,
        intervalValue = intervalValue, intervalUnit = intervalUnit,
        startDate = startDate, dueDate = dueDateOverride,
        nextDueDate = dueDateOverride ?: dueDateMillis,
        lastCompletedAt = lastCompletedAt, isActive = true,
        createdAt = 0, updatedAt = 0
    )

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S03 — LIGHT 级别首次提醒为到期当天 09:00")
        fun `firstNotify LIGHT returns dueDate 9AM`() {
            val task = createTask(ReminderLevel.LIGHT)
            val result = ReminderCalculator.firstNotifyTime(task)
            val expected = LocalDateTime.of(dueDate, LocalTime.of(9, 0))
            assertThat(result).isEqualTo(expected)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — firstNotifyTime
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — firstNotifyTime 各级别")
    inner class FirstNotifyTimeTests {

        @Test @DisplayName("C11 — LIGHT 级别：到期当天 09:00")
        fun `LIGHT = dueDate 9AM`() {
            val task = createTask(ReminderLevel.LIGHT)
            val expected = LocalDateTime.of(dueDate, LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }

        @Test @DisplayName("C12 — MEDIUM 级别：提前3天 09:00")
        fun `MEDIUM = dueDate minus 3 days 9AM`() {
            val task = createTask(ReminderLevel.MEDIUM)
            val expected = LocalDateTime.of(dueDate.minusDays(3), LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }

        @Test @DisplayName("C13 — HEAVY 级别：提前15天 09:00")
        fun `HEAVY = dueDate minus 15 days 9AM`() {
            val task = createTask(ReminderLevel.HEAVY)
            val expected = LocalDateTime.of(dueDate.minusDays(15), LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }

        @Test @DisplayName("C14 — CUSTOM 级别：提前7天 09:00")
        fun `CUSTOM = dueDate minus customAdvanceDays 9AM`() {
            val task = createTask(ReminderLevel.CUSTOM, customAdvanceDays = 7)
            val expected = LocalDateTime.of(dueDate.minusDays(7), LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }

        @Test @DisplayName("C15 — CUSTOM 级别：提前30天 09:00")
        fun `CUSTOM = dueDate minus 30 days 9AM`() {
            val task = createTask(ReminderLevel.CUSTOM, customAdvanceDays = 30)
            val expected = LocalDateTime.of(dueDate.minusDays(30), LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — nextDueDate
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — nextDueDate 循环任务")
    inner class NextDueDateTests {

        @Test @DisplayName("C16 — ONCE 任务 nextDueDate = dueDate")
        fun `ONCE nextDueDate equals dueDate`() {
            val task = createTask(ReminderLevel.LIGHT, ReminderType.ONCE)
            // 对于 ONCE 任务，nextDueDate 直接使用 dueDate
            val result = ReminderCalculator.calculateNextDueDate(task)
            assertThat(result.toLocalDate()).isEqualTo(dueDate)
        }

        @Test @DisplayName("C17 — RECURRING 每3天：完成后 nextDueDate = lastCompleted + 3天")
        fun `recurring 3 days`() {
            val completedAt = LocalDate.of(2026, 5, 10)
            val completedMillis = completedAt.toEpochDay() * 86400_000L
            val task = createTask(
                ReminderLevel.LIGHT, ReminderType.RECURRING,
                intervalValue = 3, intervalUnit = IntervalUnit.DAY,
                lastCompletedAt = completedMillis
            )
            val result = ReminderCalculator.calculateNextDueDate(task)
            assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 13))
        }

        @Test @DisplayName("C18 — RECURRING 每2周：完成后 nextDueDate = lastCompleted + 14天")
        fun `recurring 2 weeks`() {
            val completedAt = LocalDate.of(2026, 5, 1)
            val completedMillis = completedAt.toEpochDay() * 86400_000L
            val task = createTask(
                ReminderLevel.MEDIUM, ReminderType.RECURRING,
                intervalValue = 2, intervalUnit = IntervalUnit.WEEK,
                lastCompletedAt = completedMillis
            )
            val result = ReminderCalculator.calculateNextDueDate(task)
            assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 5, 15))
        }

        @Test @DisplayName("C19 — RECURRING 每月：完成后 nextDueDate = lastCompleted + 1月")
        fun `recurring 1 month`() {
            val completedAt = LocalDate.of(2026, 1, 15)
            val completedMillis = completedAt.toEpochDay() * 86400_000L
            val task = createTask(
                ReminderLevel.LIGHT, ReminderType.RECURRING,
                intervalValue = 1, intervalUnit = IntervalUnit.MONTH,
                lastCompletedAt = completedMillis
            )
            val result = ReminderCalculator.calculateNextDueDate(task)
            assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 2, 15))
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — countdownDays
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — countdownDays")
    inner class CountdownTests {

        @Test @DisplayName("C20 — countdownDays 准确计算")
        fun `countdownDays correct`() {
            val task = createTask(ReminderLevel.MEDIUM)
            val now = LocalDate.of(2026, 5, 15)
            val daysLeft = ReminderCalculator.countdownDays(task, now)
            assertThat(daysLeft).isEqualTo(5) // 5月20日 - 5月15日 = 5
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 回归测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("回归测试 — 边界条件")
    inner class RegressionTests {

        @Test @DisplayName("R06 — intervalValue=0 不抛异常")
        fun `intervalValue zero does not throw`() {
            val task = createTask(
                ReminderLevel.LIGHT, ReminderType.RECURRING,
                intervalValue = 0,
                lastCompletedAt = (dueDate.toEpochDay() * 86400_000L)
            )
            val result = ReminderCalculator.calculateNextDueDate(task)
            // 加上0天等于同一天
            assertThat(result.toLocalDate()).isEqualTo(dueDate)
        }

        @Test @DisplayName("R07 — intervalValue 负数")
        fun `intervalValue negative`() {
            val task = createTask(
                ReminderLevel.LIGHT, ReminderType.RECURRING,
                intervalValue = -1,
                lastCompletedAt = (dueDate.toEpochDay() * 86400_000L)
            )
            // API 预期：不抛异常，result < dueDate
            val result = ReminderCalculator.calculateNextDueDate(task)
            assertThat(result.toLocalDate().toEpochDay()).isLessThan(dueDate.toEpochDay())
        }

        @Test @DisplayName("R08 — CUSTOM customAdvanceDays=0 (当天提醒)")
        fun `customAdvanceDays zero`() {
            val task = createTask(ReminderLevel.CUSTOM, customAdvanceDays = 0)
            val expected = LocalDateTime.of(dueDate, LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }

        @Test @DisplayName("R09 — CUSTOM customAdvanceDays=365 (提前一年)")
        fun `customAdvanceDays 365`() {
            val task = createTask(ReminderLevel.CUSTOM, customAdvanceDays = 365)
            val expected = LocalDateTime.of(dueDate.minusDays(365), LocalTime.of(9, 0))
            assertThat(ReminderCalculator.firstNotifyTime(task)).isEqualTo(expected)
        }

        @Test @DisplayName("R10 — ONCE 任务 intervalUnit=null 不抛异常")
        fun `ONCE with null intervalUnit does not throw`() {
            val task = createTask(
                ReminderLevel.LIGHT, ReminderType.ONCE,
                intervalValue = null, intervalUnit = null
            )
            assertThat(ReminderCalculator.firstNotifyTime(task)).isNotNull
        }
    }
}