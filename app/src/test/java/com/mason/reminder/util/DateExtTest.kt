package com.mason.reminder.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@DisplayName("DateExt — 日期工具扩展")
class DateExtTest {

    // ═══════════════════════════════════════════════════════════════
    // 冒烟测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("冒烟测试")
    inner class SmokeTests {

        @Test
        @DisplayName("S04 — daysUntil 同一天返回 0")
        fun `daysUntil same day returns 0`() {
            val date = LocalDate.of(2026, 5, 15)
            assertThat(date.daysUntil(date)).isEqualTo(0)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — Long ↔ LocalDate
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — 类型转换")
    inner class ConversionTests {

        private val refDate = LocalDate.of(2026, 5, 15)
        private val refMillis = refDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        @Test @DisplayName("C21 — toLocalDate 正确转换")
        fun `toLocalDate correct`() {
            assertThat(refMillis.toLocalDate()).isEqualTo(refDate)
        }

        @Test @DisplayName("C22 — toEpochMillis 往返一致性")
        fun `toEpochMillis roundtrip`() {
            val roundtrip = refDate.toEpochMillis().toLocalDate()
            assertThat(roundtrip).isEqualTo(refDate)
        }

        @Test @DisplayName("C21b — toLocalDateTime 正确转换")
        fun `toLocalDateTime correct`() {
            val ldt = refMillis.toLocalDateTime()
            assertThat(ldt.toLocalDate()).isEqualTo(refDate)
            assertThat(ldt.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — daysUntil
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — daysUntil")
    inner class DaysUntilTests {

        @Test @DisplayName("C23 — daysUntil 正数 (5天后)")
        fun `daysUntil positive`() {
            val dueDate = LocalDate.of(2026, 5, 20)
            val now = LocalDate.of(2026, 5, 15)
            assertThat(dueDate.daysUntil(now)).isEqualTo(5)
        }

        @Test @DisplayName("C24 — daysUntil 今天")
        fun `daysUntil today`() {
            val today = LocalDate.of(2026, 5, 15)
            assertThat(today.daysUntil(today)).isEqualTo(0)
        }

        @Test @DisplayName("C25 — daysUntil 负数 (已过期)")
        fun `daysUntil negative`() {
            val past = LocalDate.of(2026, 5, 10)
            val now = LocalDate.of(2026, 5, 15)
            // past.daysUntil(now) = past_day - now_day = 10 - 15 = -5? No...
            // Actually: past.toEpochDay() - now.toEpochDay() = past - now
            // past=10, now=15 → 10-15 = -5
            assertThat(past.daysUntil(now)).isEqualTo(-5)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — 格式化
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — 格式化")
    inner class FormatTests {

        @Test @DisplayName("C26 — formatDisplay 中文格式")
        fun `formatDisplay Chinese`() {
            val date = LocalDate.of(2026, 5, 15)
            assertThat(date.formatDisplay()).isEqualTo("5月15日")
        }

        @Test @DisplayName("C27 — formatDisplayFull 含年份")
        fun `formatDisplayFull with year`() {
            val date = LocalDate.of(2026, 5, 15)
            assertThat(date.formatDisplayFull()).isEqualTo("2026年5月15日")
        }

        @Test @DisplayName("C28 — formatISO 标准格式")
        fun `formatISO standard`() {
            val date = LocalDate.of(2026, 5, 15)
            assertThat(date.formatISO()).isEqualTo("2026-05-15")
        }

        @Test @DisplayName("C30 — formatDisplayDate (Long) 格式化")
        fun `formatDisplayDate Long`() {
            val date = LocalDate.of(2026, 5, 15)
            val millis = date.toEpochMillis()
            assertThat(millis.formatDisplayDate()).isEqualTo("5月15日")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 覆盖测试 — 时间边界
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("覆盖测试 — 时间边界")
    inner class BoundaryTests {

        @Test @DisplayName("C29 — toStartOfDay 返回当天 00:00:00")
        fun `toStartOfDay returns midnight`() {
            val date = LocalDate.of(2026, 5, 15)
            val result = date.toStartOfDay()
            assertThat(result.toLocalDate()).isEqualTo(date)
            assertThat(result.toLocalTime()).isEqualTo(LocalTime.MIDNIGHT)
        }

        @Test @DisplayName("C29b — toEndOfDay 返回当天 23:59:59.999")
        fun `toEndOfDay returns end of day`() {
            val date = LocalDate.of(2026, 5, 15)
            val result = date.toEndOfDay()
            assertThat(result.toLocalDate()).isEqualTo(date)
            assertThat(result.toLocalTime()).isEqualTo(LocalTime.MAX)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 回归测试
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("回归测试 — 边界条件")
    inner class RegressionTests {

        @Test @DisplayName("R11 — epochMillis=0 (1970-01-01)")
        fun `epoch zero`() {
            val zero = 0L
            val date = zero.toLocalDate()
            // 系统时区 UTC+8 → 1970-01-01
            assertThat(date.year).isEqualTo(1970)
            assertThat(date.monthValue).isEqualTo(1)
            assertThat(date.dayOfMonth).isEqualTo(1)
        }

        @Test @DisplayName("R12 — epochMillis=Long.MAX_VALUE")
        fun `epoch max value`() {
            // 极远未来，不抛异常即可
            val result = Long.MAX_VALUE.toLocalDateTime()
            assertThat(result).isNotNull
        }

        @Test @DisplayName("R14 — 单数月份和日期格式 (1月1日)")
        fun `single digit month day`() {
            val date = LocalDate.of(2026, 1, 1)
            assertThat(date.formatDisplay()).isEqualTo("1月1日")
        }
    }
}