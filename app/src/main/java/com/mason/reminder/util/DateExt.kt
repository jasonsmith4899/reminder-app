package com.mason.reminder.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 日期工具扩展函数。纯 java.time，无外部依赖。
 */

// ── Long ↔ LocalDate / LocalDateTime ──

/** epoch millis → LocalDate（默认系统时区） */
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}

/** epoch millis → LocalDateTime */
fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
}

/** LocalDate → epoch millis（当天起始 00:00） */
fun LocalDate.toEpochMillis(): Long {
    return this.atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

/** LocalDateTime → epoch millis */
fun LocalDateTime.toEpochMillis(): Long {
    return this.atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

// ── 日期计算 ──

/** 两个 LocalDate 之间的天数差（正数 = dueDate 在 now 之后） */
fun LocalDate.daysUntil(now: LocalDate): Long {
    return this.toEpochDay() - now.toEpochDay()
}

/** LocalDate 的当天起始 LocalDateTime */
fun LocalDate.toStartOfDay(): LocalDateTime {
    return this.atStartOfDay()
}

/** LocalDate 的当天结束 LocalDateTime（次日 00:00 前） */
fun LocalDate.toEndOfDay(): LocalDateTime {
    return this.atTime(LocalTime.MAX)
}

// ── 格式化 ──

/** 格式化为显示文本：2026-05-14 → "5月14日" */
fun LocalDate.formatDisplay(): String {
    val formatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINESE)
    return this.format(formatter)
}

/** 格式化为显示文本（含年份）：2026-05-14 → "2026年5月14日" */
fun LocalDate.formatDisplayFull(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINESE)
    return this.format(formatter)
}

/** 格式化为 ISO 格式：2026-05-14 */
fun LocalDate.formatISO(): String {
    return this.format(DateTimeFormatter.ISO_LOCAL_DATE)
}

/** epoch millis → 显示文本 */
fun Long.formatDisplayDate(): String {
    return this.toLocalDate().formatDisplay()
}