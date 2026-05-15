package com.mason.reminder.data.model

import androidx.compose.ui.graphics.Color

enum class UrgencyState {
    CALM,    // 无紧急 → 蓝绿色
    NOTICE,  // 3天内 → 黄色
    URGENT,  // 1天内 → 橙色
    CRITICAL // 今天 → 红色
}

fun UrgencyState.color(): Color = when (this) {
    UrgencyState.CALM    -> Color(0xFF2E8B57)  // SeaGreen
    UrgencyState.NOTICE  -> Color(0xFFFFC107)  // Amber
    UrgencyState.URGENT  -> Color(0xFFFF9800)  // Orange
    UrgencyState.CRITICAL -> Color(0xFFF44336) // Red
}

fun UrgencyState.colorHex(): String = when (this) {
    UrgencyState.CALM    -> "#2E8B57"
    UrgencyState.NOTICE  -> "#FFC107"
    UrgencyState.URGENT  -> "#FF9800"
    UrgencyState.CRITICAL -> "#F44336"
}

fun UrgencyState.label(): String = when (this) {
    UrgencyState.CALM    -> "安心"
    UrgencyState.NOTICE  -> "注意"
    UrgencyState.URGENT  -> "紧迫"
    UrgencyState.CRITICAL -> "紧急"
}