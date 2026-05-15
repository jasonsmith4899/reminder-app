package com.mason.reminder.data.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@DisplayName("UrgencyState — 紧急度枚举")
class UrgencyStateTest {

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S06 — color() 返回非空")
        fun `color returns non-null`() {
            UrgencyState.entries.forEach { state ->
                assertThat(state.color()).isNotNull
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — color()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — color()")
    inner class ColorTests {

        @Test @DisplayName("C36 — CALM.color() = #FF2E8B57")
        fun `CALM color`() {
            assertThat(UrgencyState.CALM.color().toArgb()).isEqualTo(0xFF_2E8B57.toInt())
        }

        @Test @DisplayName("C37 — NOTICE.color() = #FFFFC107")
        fun `NOTICE color`() {
            assertThat(UrgencyState.NOTICE.color().toArgb()).isEqualTo(0xFF_FFC107.toInt())
        }

        @Test @DisplayName("C38 — URGENT.color() = #FFFF9800")
        fun `URGENT color`() {
            assertThat(UrgencyState.URGENT.color().toArgb()).isEqualTo(0xFF_FF9800.toInt())
        }

        @Test @DisplayName("C39 — CRITICAL.color() = #FFF44336")
        fun `CRITICAL color`() {
            assertThat(UrgencyState.CRITICAL.color().toArgb()).isEqualTo(0xFF_F44336.toInt())
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — label()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — label()")
    inner class LabelTests {

        @Test @DisplayName("C40 — CALM.label() = 安心")
        fun `CALM label`() {
            assertThat(UrgencyState.CALM.label()).isEqualTo("安心")
        }

        @Test @DisplayName("C41 — NOTICE.label() = 注意")
        fun `NOTICE label`() {
            assertThat(UrgencyState.NOTICE.label()).isEqualTo("注意")
        }

        @Test @DisplayName("C42 — URGENT.label() = 紧迫")
        fun `URGENT label`() {
            assertThat(UrgencyState.URGENT.label()).isEqualTo("紧迫")
        }

        @Test @DisplayName("C43 — CRITICAL.label() = 紧急")
        fun `CRITICAL label`() {
            assertThat(UrgencyState.CRITICAL.label()).isEqualTo("紧急")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — colorHex()
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — colorHex()")
    inner class ColorHexTests {

        @Test @DisplayName("C44 — colorHex 返回正确的 hex 格式")
        fun `colorHex format`() {
            val hexes = mapOf(
                UrgencyState.CALM to "#2E8B57",
                UrgencyState.NOTICE to "#FFC107",
                UrgencyState.URGENT to "#FF9800",
                UrgencyState.CRITICAL to "#F44336"
            )
            hexes.forEach { (state, expected) ->
                assertThat(state.colorHex()).isEqualTo(expected)
                    .withFailMessage("${state.name}.colorHex() should be $expected")
            }
        }

        @Test @DisplayName("C44b — colorHex 以 # 开头且长度 7")
        fun `colorHex starts with hash`() {
            UrgencyState.entries.forEach { state ->
                val hex = state.colorHex()
                assertThat(hex).startsWith("#")
                assertThat(hex).hasSize(7)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — ordinal
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — ordinal 顺序")
    inner class OrdinalTests {

        @Test
        @DisplayName("CALM ordinal < NOTICE < URGENT < CRITICAL")
        fun `ordinal ordering`() {
            assertThat(UrgencyState.CALM.ordinal).isLessThan(UrgencyState.NOTICE.ordinal)
            assertThat(UrgencyState.NOTICE.ordinal).isLessThan(UrgencyState.URGENT.ordinal)
            assertThat(UrgencyState.URGENT.ordinal).isLessThan(UrgencyState.CRITICAL.ordinal)
        }
    }
}