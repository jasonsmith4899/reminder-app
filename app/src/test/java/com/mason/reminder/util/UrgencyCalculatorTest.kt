package com.mason.reminder.util

import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("UrgencyCalculator — 紧急度计算")
class UrgencyCalculatorTest {

    private val today = LocalDate.of(2026, 5, 15)

    private fun taskWithDaysLeft(daysLeft: Long): Task {
        val dueDate = today.plusDays(daysLeft)
        return Task(
            id = 1, title = "测试任务", description = "",
            categoryId = 1, reminderType = ReminderType.ONCE,
            reminderLevel = ReminderLevel.LIGHT,
            customAdvanceDays = null, customNotifyFreq = null,
            intervalValue = null, intervalUnit = null,
            startDate = null, dueDate = dueDate.toEpochDay() * 86400_000,
            nextDueDate = dueDate.toEpochDay() * 86400_000,
            lastCompletedAt = null, isActive = true,
            createdAt = 0, updatedAt = 0
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S01 — calculate 返回非空结果")
        fun `calculate returns non-null for valid Task`() {
            val task = taskWithDaysLeft(3)
            val result = UrgencyCalculator.calculate(task, today)
            assertThat(result).isNotNull
        }

        @Test
        @DisplayName("S02 — maxUrgency 对空列表返回 CALM")
        fun `maxUrgency returns CALM for empty list`() {
            val result = UrgencyCalculator.maxUrgency(emptyList(), today)
            assertThat(result).isEqualTo(UrgencyState.CALM)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — calculate 各边界值")
    inner class CalculateBoundaryTests {

        @Test @DisplayName("C01 — 今天到期 → CRITICAL")
        fun `today due returns CRITICAL`() {
            val task = taskWithDaysLeft(0)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.CRITICAL)
        }

        @Test @DisplayName("C02 — 明天到期 (daysLeft=1) → URGENT")
        fun `tomorrow due returns URGENT`() {
            val task = taskWithDaysLeft(1)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.URGENT)
        }

        @Test @DisplayName("C03 — 2天后到期 → NOTICE")
        fun `two days left returns NOTICE`() {
            val task = taskWithDaysLeft(2)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.NOTICE)
        }

        @Test @DisplayName("C04 — 3天后到期 → NOTICE")
        fun `three days left returns NOTICE`() {
            val task = taskWithDaysLeft(3)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.NOTICE)
        }

        @Test @DisplayName("C05 — 4天后到期 → CALM")
        fun `four days left returns CALM`() {
            val task = taskWithDaysLeft(4)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.CALM)
        }

        @Test @DisplayName("C06 — 已过期1天 → CRITICAL")
        fun `expired one day returns CRITICAL`() {
            val task = taskWithDaysLeft(-1)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.CRITICAL)
        }

        @Test @DisplayName("C07 — 已过期7天 → CRITICAL")
        fun `expired seven days returns CRITICAL`() {
            val task = taskWithDaysLeft(-7)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.CRITICAL)
        }

        @Test @DisplayName("C08 — 半年后到期 → CALM")
        fun `six months away returns CALM`() {
            val task = taskWithDaysLeft(180)
            assertThat(UrgencyCalculator.calculate(task, today)).isEqualTo(UrgencyState.CALM)
        }
    }

    @Nested
    @DisplayName("覆盖测试 — maxUrgency")
    inner class MaxUrgencyTests {

        @Test @DisplayName("C09 — maxUrgency 取最高级别 (混合所有状态)")
        fun `maxUrgency returns highest among mixed states`() {
            val tasks = listOf(
                taskWithDaysLeft(180),  // CALM
                taskWithDaysLeft(3),    // NOTICE
                taskWithDaysLeft(0),    // CRITICAL
                taskWithDaysLeft(2)     // NOTICE
            )
            assertThat(UrgencyCalculator.maxUrgency(tasks, today)).isEqualTo(UrgencyState.CRITICAL)
        }

        @Test @DisplayName("C10 — maxUrgency 全 CALM")
        fun `maxUrgency all calm returns CALM`() {
            val tasks = listOf(
                taskWithDaysLeft(30),
                taskWithDaysLeft(60)
            )
            assertThat(UrgencyCalculator.maxUrgency(tasks, today)).isEqualTo(UrgencyState.CALM)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 回归测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("回归测试 — 边界条件")
    inner class RegressionTests {

        @Test @DisplayName("R01 — 闰年 2月29日 正确计算")
        fun `leap year feb 29`() {
            val feb29 = LocalDate.of(2028, 2, 29)
            val feb25 = LocalDate.of(2028, 2, 25)
            val task = taskWithDaysLeft(0).copy(
                nextDueDate = feb29.toEpochDay() * 86400_000
            )
            val result = UrgencyCalculator.calculate(task, feb25)
            assertThat(result).isNotNull
            // 4天后 → CALM
            assertThat(result).isEqualTo(UrgencyState.CALM)
        }

        @Test @DisplayName("R02 — 跨年边界：12月31日到期，1月1日检查")
        fun `cross year boundary`() {
            val dec31 = LocalDate.of(2026, 12, 31)
            val jan1 = LocalDate.of(2027, 1, 1)
            val task = taskWithDaysLeft(0).copy(
                nextDueDate = dec31.toEpochDay() * 86400_000
            )
            // 1月1日检查已过期1天
            val result = UrgencyCalculator.calculate(task, jan1)
            assertThat(result).isEqualTo(UrgencyState.CRITICAL)
        }

        @Test @DisplayName("R04 — isActive=false 不计入 maxUrgency")
        fun `inactive task excluded from maxUrgency`() {
            val activeTask = taskWithDaysLeft(10) // CALM
            val inactiveTask = taskWithDaysLeft(0).copy(isActive = false)
            val result = UrgencyCalculator.maxUrgency(listOf(activeTask, inactiveTask), today)
            assertThat(result).isEqualTo(UrgencyState.CALM)
        }

        @Test @DisplayName("R05 — 1000个任务性能不降级")
        fun `maxUrgency with 1000 tasks performs correctly`() {
            val tasks = (1..1000).map { i ->
                taskWithDaysLeft(i.toLong() % 200 - 100) // range: -100 ~ 99
                    .copy(id = i.toLong())
            }
            val startTime = System.nanoTime()
            val result = UrgencyCalculator.maxUrgency(tasks, today)
            val durationMs = (System.nanoTime() - startTime) / 1_000_000
            // 有已过期 → CRITICAL
            assertThat(result).isEqualTo(UrgencyState.CRITICAL)
            // 应该在 100ms 内完成
            assertThat(durationMs).isLessThan(100)
        }
    }
}