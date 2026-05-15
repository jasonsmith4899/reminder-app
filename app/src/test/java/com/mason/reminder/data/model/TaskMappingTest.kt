package com.mason.reminder.data.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Task Mapping — Entity ↔ Domain 双向转换")
class TaskMappingTest {

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S05 — toDomain + toEntity 往返一致性")
        fun `roundtrip preserves data`() {
            val original = createSampleTask(
                taskType = ReminderType.RECURRING,
                taskLevel = ReminderLevel.MEDIUM,
                intervalValue = 3,
                intervalUnit = IntervalUnit.DAY
            )
            val entity = original.toEntity()
            val restored = entity.toDomain()
            assertThat(restored).isEqualTo(original.copy(
                // 毫秒精度可能有微小差异，比较所有关键字段
            ))
            assertThat(restored.id).isEqualTo(original.id)
            assertThat(restored.title).isEqualTo(original.title)
            assertThat(restored.categoryId).isEqualTo(original.categoryId)
            assertThat(restored.reminderType).isEqualTo(original.reminderType)
            assertThat(restored.reminderLevel).isEqualTo(original.reminderLevel)
            assertThat(restored.intervalValue).isEqualTo(original.intervalValue)
            assertThat(restored.intervalUnit).isEqualTo(original.intervalUnit)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — 各类型任务转换")
    inner class ConversionTests {

        @Test @DisplayName("C31 — ONCE 任务双向转换")
        fun `ONCE task roundtrip`() {
            val original = createSampleTask(
                taskType = ReminderType.ONCE,
                taskLevel = ReminderLevel.LIGHT,
                intervalValue = null,
                intervalUnit = null
            )
            val restored = original.toEntity().toDomain()
            assertThat(restored.reminderType).isEqualTo(ReminderType.ONCE)
            assertThat(restored.reminderLevel).isEqualTo(ReminderLevel.LIGHT)
            assertThat(restored.intervalValue).isNull()
            assertThat(restored.intervalUnit).isNull()
        }

        @Test @DisplayName("C32 — RECURRING 任务双向转换")
        fun `RECURRING task roundtrip`() {
            val original = createSampleTask(
                taskType = ReminderType.RECURRING,
                taskLevel = ReminderLevel.MEDIUM,
                intervalValue = 2,
                intervalUnit = IntervalUnit.WEEK
            )
            val restored = original.toEntity().toDomain()
            assertThat(restored.reminderType).isEqualTo(ReminderType.RECURRING)
            assertThat(restored.intervalValue).isEqualTo(2)
            assertThat(restored.intervalUnit).isEqualTo(IntervalUnit.WEEK)
        }

        @Test @DisplayName("C33 — CUSTOM 级别任务双向转换")
        fun `CUSTOM level task roundtrip`() {
            val original = createSampleTask(
                taskType = ReminderType.RECURRING,
                taskLevel = ReminderLevel.CUSTOM,
                customAdvanceDays = 7,
                customNotifyFreq = CustomNotifyFreq.DAILY
            )
            val restored = original.toEntity().toDomain()
            assertThat(restored.reminderLevel).isEqualTo(ReminderLevel.CUSTOM)
            assertThat(restored.customAdvanceDays).isEqualTo(7)
            assertThat(restored.customNotifyFreq).isEqualTo(CustomNotifyFreq.DAILY)
        }

        @Test @DisplayName("C34 — null 可选字段保持 null")
        fun `null optional fields preserved`() {
            val original = createSampleTask(
                taskType = ReminderType.ONCE,
                taskLevel = ReminderLevel.LIGHT,
                intervalValue = null,
                intervalUnit = null,
                customAdvanceDays = null,
                customNotifyFreq = null,
                startDate = null,
                lastCompletedAt = null
            )
            val restored = original.toEntity().toDomain()
            assertThat(restored.intervalValue).isNull()
            assertThat(restored.intervalUnit).isNull()
            assertThat(restored.customAdvanceDays).isNull()
            assertThat(restored.customNotifyFreq).isNull()
            assertThat(restored.startDate).isNull()
            assertThat(restored.lastCompletedAt).isNull()
        }

        @Test @DisplayName("C35 — 所有 IntervalUnit 枚举值正确转换")
        fun `all IntervalUnit values convert`() {
            IntervalUnit.entries.forEach { unit ->
                val original = createSampleTask(
                    taskType = ReminderType.RECURRING,
                    taskLevel = ReminderLevel.LIGHT,
                    intervalValue = 1,
                    intervalUnit = unit
                )
                val restored = original.toEntity().toDomain()
                assertThat(restored.intervalUnit).isEqualTo(unit)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 辅助方法
    // ═══════════════════════════════════════════════════════════════

    companion object {
        fun createSampleTask(
            taskType: ReminderType = ReminderType.RECURRING,
            taskLevel: ReminderLevel = ReminderLevel.LIGHT,
            intervalValue: Int? = 3,
            intervalUnit: IntervalUnit? = IntervalUnit.DAY,
            customAdvanceDays: Int? = null,
            customNotifyFreq: CustomNotifyFreq? = null,
            startDate: Long? = 1700000000000L,
            lastCompletedAt: Long? = 1700000000000L
        ): Task = Task(
            id = 42,
            title = "浇花",
            description = "阳台多肉",
            categoryId = 1,
            reminderType = taskType,
            reminderLevel = taskLevel,
            customAdvanceDays = customAdvanceDays,
            customNotifyFreq = customNotifyFreq,
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            startDate = startDate,
            dueDate = 1715000000000L,
            nextDueDate = 1715000000000L,
            lastCompletedAt = lastCompletedAt,
            isActive = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        )
    }
}