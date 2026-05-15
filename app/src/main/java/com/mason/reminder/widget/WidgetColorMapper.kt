package com.mason.reminder.widget

import com.mason.reminder.data.model.UrgencyState

/**
 * UrgencyState → 颜色值映射，用于 Widget / 图标变色。
 *
 * 颜色规范：
 * - CALM    → #2E8B57（海绿色，稳定安心）
 * - NOTICE  → #FFC107（琥珀黄，引起注意）
 * - URGENT  → #FF9800（橙色，紧迫感）
 * - CRITICAL→ #F44336（红色，必须立刻处理）
 */
object WidgetColorMapper {

    private const val CALM_COLOR    = 0xFF2E8B57.toInt()  // SeaGreen
    private const val NOTICE_COLOR  = 0xFFFFC107.toInt()  // Amber
    private const val URGENT_COLOR  = 0xFFFF9800.toInt()  // Orange
    private const val CRITICAL_COLOR = 0xFFF44336.toInt() // Red

    /** UrgencyState → ARGB 颜色值 */
    fun colorFor(state: UrgencyState): Int {
        return when (state) {
            UrgencyState.CALM    -> CALM_COLOR
            UrgencyState.NOTICE  -> NOTICE_COLOR
            UrgencyState.URGENT  -> URGENT_COLOR
            UrgencyState.CRITICAL -> CRITICAL_COLOR
        }
    }

    /** UrgencyState → HEX 字符串（不含 #） */
    fun hexFor(state: UrgencyState): String {
        return when (state) {
            UrgencyState.CALM    -> "2E8B57"
            UrgencyState.NOTICE  -> "FFC107"
            UrgencyState.URGENT  -> "FF9800"
            UrgencyState.CRITICAL -> "F44336"
        }
    }

    /** UrgencyState → 含 # 的 HEX 字符串 */
    fun colorHexForState(state: UrgencyState): String {
        return when (state) {
            UrgencyState.CALM    -> "#2E8B57"
            UrgencyState.NOTICE  -> "#FFC107"
            UrgencyState.URGENT  -> "#FF9800"
            UrgencyState.CRITICAL -> "#F44336"
        }
    }

    /** ARGB 颜色值 → UrgencyState（反向映射） */
    fun stateForColor(color: Int): UrgencyState {
        return when (color) {
            CALM_COLOR    -> UrgencyState.CALM
            NOTICE_COLOR  -> UrgencyState.NOTICE
            URGENT_COLOR  -> UrgencyState.URGENT
            CRITICAL_COLOR -> UrgencyState.CRITICAL
            else          -> UrgencyState.CALM // 未知颜色默认平静
        }
    }
}