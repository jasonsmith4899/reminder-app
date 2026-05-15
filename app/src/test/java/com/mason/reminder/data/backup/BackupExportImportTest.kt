package com.mason.reminder.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mason.reminder.data.db.entity.CategoryEntity
import com.mason.reminder.data.db.entity.ReminderLogEntity
import com.mason.reminder.data.db.entity.TaskEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 备份导出/导入逻辑测试 — 纯 JVM 单元测试（无需 Room 数据库）。
 *
 * 重点测试：
 * 1. BackupData 的 Gson 序列化/反序列化
 * 2. JSON 格式完整性
 * 3. 边界条件（空数据、特殊字符、损坏 JSON）
 */
@DisplayName("Backup — 备份导出导入")
class BackupExportImportTest {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private val sampleCategory = CategoryEntity(
        id = 1, name = "植物养护", iconName = "eco",
        colorHex = "#2E8B57", sortOrder = 1
    )

    private val sampleTask = TaskEntity(
        id = 1, title = "浇花🌸", description = "阳台多肉第3盆",
        categoryId = 1, reminderType = "RECURRING",
        reminderLevel = "MEDIUM",
        intervalValue = 3, intervalUnit = "DAY",
        nextDueDate = 1715000000000L, isActive = true
    )

    private val sampleLog = ReminderLogEntity(
        id = 1, taskId = 1,
        notifiedAt = 1714900000000L,
        daysBeforeDue = 3, actionTaken = "COMPLETED"
    )

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S08 — BackupData 导出非空 JSON")
        fun `export produces non-empty JSON`() {
            val data = BackupData(
                categories = listOf(sampleCategory),
                tasks = listOf(sampleTask),
                reminderLogs = listOf(sampleLog)
            )
            val json = gson.toJson(data)
            assertThat(json).isNotNull
            assertThat(json).isNotBlank
            // 有数据的话 JSON 不可能太短
            assertThat(json.length).isGreaterThan(50)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // R23-R26: 回归测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("回归测试 — R23 空数据库导出")
    inner class EmptyBackupTests {

        @Test
        @DisplayName("R23 — 空数据导出后能反序列化")
        fun `empty database roundtrip`() {
            val data = BackupData(
                categories = emptyList(),
                tasks = emptyList(),
                reminderLogs = emptyList()
            )
            val json = gson.toJson(data)
            assertThat(json).isNotBlank

            val restored = gson.fromJson(json, BackupData::class.java)
            assertThat(restored).isNotNull
            assertThat(restored.categories).isEmpty()
            assertThat(restored.tasks).isEmpty()
            assertThat(restored.reminderLogs).isEmpty()
        }
    }

    @Nested
    @DisplayName("回归测试 — R24 大量数据")
    inner class LargeDataTests {

        @Test
        @DisplayName("R24 — 大量数据 (100 分类 × 10 任务) JSON 完整")
        fun `large dataset JSON complete`() {
            val categories = (1..100).map { i ->
                sampleCategory.copy(id = i.toLong(), name = "分类$i")
            }
            val tasks = (1..1000).map { i ->
                sampleTask.copy(
                    id = i.toLong(),
                    categoryId = (i / 10 + 1).toLong(),
                    title = "任务$i"
                )
            }
            val data = BackupData(
                categories = categories,
                tasks = tasks,
                reminderLogs = emptyList()
            )
            val json = gson.toJson(data)

            // 反序列化回来
            val restored = gson.fromJson(json, BackupData::class.java)
            assertThat(restored.categories).hasSize(100)
            assertThat(restored.tasks).hasSize(1000)
        }
    }

    @Nested
    @DisplayName("回归测试 — R25 特殊字符")
    inner class SpecialCharTests {

        @Test
        @DisplayName("R25 — 特殊字符标题正确序列化/反序列化")
        fun `special characters in title`() {
            val task = sampleTask.copy(
                title = "浇花🌸 第3盆 \uD83D\uDC31 猫猫",
                description = "包含\"双引号\"和\\反斜杠"
            )
            val data = BackupData(
                categories = listOf(sampleCategory),
                tasks = listOf(task),
                reminderLogs = emptyList()
            )
            val json = gson.toJson(data)

            val restored = gson.fromJson(json, BackupData::class.java)
            assertThat(restored.tasks[0].title).isEqualTo("浇花🌸 第3盆 \uD83D\uDC31 猫猫")
            assertThat(restored.tasks[0].description).isEqualTo("包含\"双引号\"和\\反斜杠")
        }
    }

    @Nested
    @DisplayName("回归测试 — R26 损坏 JSON")
    inner class CorruptedJsonTests {

        @Test
        @DisplayName("R26 — 非法 JSON 返回 Error")
        fun `corrupted JSON returns error`() {
            val badJson = "{这不是合法的JSON"

            val result = try {
                gson.fromJson(badJson, BackupData::class.java)
            } catch (e: Exception) {
                null
            }

            // Gson 对损坏的 JSON 抛异常，这是预期行为
            // Importer 中会捕获并转换为 ImportResult.Error
            assertThat(result).isNull() // fromJson 应该抛异常
        }

        @Test
        @DisplayName("R26b — 空 JSON 对象")
        fun `empty JSON object`() {
            val emptyJson = "{}"
            val result = try {
                gson.fromJson(emptyJson, BackupData::class.java)
            } catch (e: Exception) {
                null
            }

            // Gson 对空 JSON 对象反序列化到 BackupData 不会抛异常
            // 但所有字段都是 null/默认值
            assertThat(result).isNotNull
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 额外覆盖测试 — BackupData 本体
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("JSON 格式完整性")
    inner class JsonFormatTests {

        @Test
        @DisplayName("BackupData JSON 包含 version 字段")
        fun `JSON includes version`() {
            val data = BackupData(
                categories = listOf(sampleCategory),
                tasks = emptyList(),
                reminderLogs = emptyList()
            )
            val json = gson.toJson(data)
            assertThat(json).contains(""""version": 1""")
        }

        @Test
        @DisplayName("BackupData JSON 包含 exportedAt 字段")
        fun `JSON includes exportedAt`() {
            val data = BackupData(
                categories = emptyList(),
                tasks = emptyList(),
                reminderLogs = emptyList()
            )
            val json = gson.toJson(data)
            assertThat(json).contains("exportedAt")
        }

        @Test
        @DisplayName("BackupData 往返一致性")
        fun `BackupData roundtrip`() {
            val original = BackupData(
                categories = listOf(sampleCategory),
                tasks = listOf(sampleTask),
                reminderLogs = listOf(sampleLog),
                version = 1,
                exportedAt = 1715000000000L
            )
            val json = gson.toJson(original)
            val restored = gson.fromJson(json, BackupData::class.java)

            assertThat(restored.version).isEqualTo(original.version)
            assertThat(restored.exportedAt).isEqualTo(original.exportedAt)
            assertThat(restored.categories).hasSize(original.categories.size)
            assertThat(restored.tasks).hasSize(original.tasks.size)
            assertThat(restored.reminderLogs).hasSize(original.reminderLogs.size)

            // 验证关键字段
            assertThat(restored.categories[0].name).isEqualTo("植物养护")
            assertThat(restored.tasks[0].title).isEqualTo("浇花🌸")
            assertThat(restored.reminderLogs[0].actionTaken).isEqualTo("COMPLETED")
        }
    }
}