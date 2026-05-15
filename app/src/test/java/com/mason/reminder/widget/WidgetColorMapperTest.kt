package com.mason.reminder.widget

import com.mason.reminder.data.model.UrgencyState
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WidgetColorMapper — Widget 颜色映射")
class WidgetColorMapperTest {

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S07 — colorFor 返回非零值")
        fun `colorFor returns non-zero`() {
            UrgencyState.entries.forEach { state ->
                assertThat(WidgetColorMapper.colorFor(state))
                    .withFailMessage("colorFor(${state.name}) should be non-zero")
                    .isNotZero
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — colorFor()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — colorFor")
    inner class ColorForTests {

        @Test @DisplayName("C45 — colorFor(CALM) = 0xFF2E8B57")
        fun `CALM color`() {
            assertThat(WidgetColorMapper.colorFor(UrgencyState.CALM))
                .isEqualTo(0xFF2E8B57.toInt())
        }

        @Test @DisplayName("C46 — colorFor(NOTICE) = 0xFFFFC107")
        fun `NOTICE color`() {
            assertThat(WidgetColorMapper.colorFor(UrgencyState.NOTICE))
                .isEqualTo(0xFFFFC107.toInt())
        }

        @Test @DisplayName("C47 — colorFor(URGENT) = 0xFFFF9800")
        fun `URGENT color`() {
            assertThat(WidgetColorMapper.colorFor(UrgencyState.URGENT))
                .isEqualTo(0xFFFF9800.toInt())
        }

        @Test @DisplayName("C48 — colorFor(CRITICAL) = 0xFFF44336")
        fun `CRITICAL color`() {
            assertThat(WidgetColorMapper.colorFor(UrgencyState.CRITICAL))
                .isEqualTo(0xFFF44336.toInt())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — hexFor()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — hexFor")
    inner class HexForTests {

        @Test @DisplayName("C49 — hexFor 返回正确的 hex 字符串")
        fun `hexFor correct`() {
            assertThat(WidgetColorMapper.hexFor(UrgencyState.CALM)).isEqualTo("#2E8B57")
            assertThat(WidgetColorMapper.hexFor(UrgencyState.NOTICE)).isEqualTo("#FFC107")
            assertThat(WidgetColorMapper.hexFor(UrgencyState.URGENT)).isEqualTo("#FF9800")
            assertThat(WidgetColorMapper.hexFor(UrgencyState.CRITICAL)).isEqualTo("#F44336")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — stateForColor() 反向映射
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — stateForColor 反向映射")
    inner class StateForColorTests {

        @Test @DisplayName("C50 — stateForColor 反向映射所有颜色")
        fun `stateForColor bidirection`() {
            UrgencyState.entries.forEach { state ->
                val color = WidgetColorMapper.colorFor(state)
                val recovered = WidgetColorMapper.stateForColor(color)
                assertThat(recovered).isEqualTo(state)
                    .withFailMessage("Bidirection failed for ${state.name}")
            }
        }

        @Test
        @DisplayName("C50b — stateForColor 未知颜色返回默认")
        fun `stateForColor unknown returns default`() {
            val result = WidgetColorMapper.stateForColor(0xFFFF0000.toInt()) // Red
            // 应该返回一个非 null 值（默认 fallback）
            assertThat(result).isNotNull
        }
    }
}