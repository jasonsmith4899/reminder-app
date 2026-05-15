package com.mason.reminder.ui.theme

import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.widget.WidgetColorMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 回归测试 — Theme.kt ARGB bug 修复验证。
 *
 * 已知 bug：CalmColor 和 CriticalColor 缺少 alpha 通道 FF，
 * 导致颜色显示错误。此测试确保修复后不再复现。
 *
 * 同时验证 Theme.kt 颜色值与 UrgencyState / WidgetColorMapper 一致。
 */
@DisplayName("Theme 颜色 — ARGB 回归测试")
class ThemeColorRegressionTest {

    // ═══════════════════════════════════════════════════════════════
    // R19-R20: Alpha 通道验证
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("R19-R20 — Alpha 通道")
    inner class AlphaChannelTests {

        @Test
        @DisplayName("R19 — CalmColor alpha = FF (0xFF2E8B57)")
        fun `CalmColor has full alpha`() {
            val color = CalmColor
            val argb = color.toArgb()
            val alpha = (argb shr 24) and 0xFF
            assertThat(alpha).isEqualTo(0xFF)
        }

        @Test
        @DisplayName("R20 — CriticalColor alpha = FF (0xFFF44336)")
        fun `CriticalColor has full alpha`() {
            val color = CriticalColor
            val argb = color.toArgb()
            val alpha = (argb shr 24) and 0xFF
            assertThat(alpha).isEqualTo(0xFF)
        }

        @Test
        @DisplayName("NoticeColor alpha = FF")
        fun `NoticeColor has full alpha`() {
            val argb = NoticeColor.toArgb()
            assertThat((argb shr 24) and 0xFF).isEqualTo(0xFF)
        }

        @Test
        @DisplayName("UrgentColor alpha = FF")
        fun `UrgentColor has full alpha`() {
            val argb = UrgentColor.toArgb()
            assertThat((argb shr 24) and 0xFF).isEqualTo(0xFF)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // R21-R22: 交叉一致性验证
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("R21-R22 — 交叉一致性")
    inner class CrossConsistencyTests {

        @Test
        @DisplayName("R21 — NoticeColor 与 UrgencyState.NOTICE.color() 一致")
        fun `NoticeColor matches UrgencyState NOTICE`() {
            assertThat(NoticeColor).isEqualTo(UrgencyState.NOTICE.color())
        }

        @Test
        @DisplayName("R22 — CalmColor 与 WidgetColorMapper.colorFor(CALM) 一致")
        fun `CalmColor matches WidgetColorMapper CALM`() {
            val calmArgb = CalmColor.toArgb()
            val mapperColor = WidgetColorMapper.colorFor(UrgencyState.CALM)
            assertThat(calmArgb).isEqualTo(mapperColor)
        }

        @Test
        @DisplayName("CriticalColor 与 WidgetColorMapper.colorFor(CRITICAL) 一致")
        fun `CriticalColor matches WidgetColorMapper CRITICAL`() {
            val criticalArgb = CriticalColor.toArgb()
            val mapperColor = WidgetColorMapper.colorFor(UrgencyState.CRITICAL)
            assertThat(criticalArgb).isEqualTo(mapperColor)
        }

        @Test
        @DisplayName("NoticeColor 与 WidgetColorMapper.colorFor(NOTICE) 一致")
        fun `NoticeColor matches WidgetColorMapper NOTICE`() {
            val noticeArgb = NoticeColor.toArgb()
            val mapperColor = WidgetColorMapper.colorFor(UrgencyState.NOTICE)
            assertThat(noticeArgb).isEqualTo(mapperColor)
        }

        @Test
        @DisplayName("UrgentColor 与 WidgetColorMapper.colorFor(URGENT) 一致")
        fun `UrgentColor matches WidgetColorMapper URGENT`() {
            val urgentArgb = UrgentColor.toArgb()
            val mapperColor = WidgetColorMapper.colorFor(UrgencyState.URGENT)
            assertThat(urgentArgb).isEqualTo(mapperColor)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 颜色值一致性：装饰色 ↔ UrgencyState
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Theme 内颜色值正确性")
    inner class ColorValueTests {

        @Test
        @DisplayName("CalmColor 值 = 0xFF2E8B57")
        fun `CalmColor value is correct`() {
            assertThat(CalmColor.toArgb()).isEqualTo(0xFF2E8B57.toInt())
        }

        @Test
        @DisplayName("NoticeColor 值 = 0xFFFFC107")
        fun `NoticeColor value is correct`() {
            assertThat(NoticeColor.toArgb()).isEqualTo(0xFFFFC107.toInt())
        }

        @Test
        @DisplayName("UrgentColor 值 = 0xFFFF9800")
        fun `UrgentColor value is correct`() {
            assertThat(UrgentColor.toArgb()).isEqualTo(0xFFFF9800.toInt())
        }

        @Test
        @DisplayName("CriticalColor 值 = 0xFFF44336")
        fun `CriticalColor value is correct`() {
            assertThat(CriticalColor.toArgb()).isEqualTo(0xFFF44336.toInt())
        }
    }
}