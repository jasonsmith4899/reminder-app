package com.mason.reminder.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.color

/**
 * 倒计时文字组件，颜色随紧急度变化。
 *
 * - daysLeft > 3  → "还剩 X 天"  (灰/默认色)
 * - daysLeft 2~3  → "还剩 X 天"  (NOTICE 黄色)
 * - daysLeft 1    → "明天到期"    (URGENT 橙色)
 * - daysLeft 0    → "今天到期"    (CRITICAL 红色)
 * - daysLeft < 0  → "已过期 X 天" (CRITICAL 红色)
 */
@Composable
fun CountdownText(
    daysLeft: Long,
    urgency: UrgencyState,
    modifier: Modifier = Modifier
) {
    val text = when {
        daysLeft > 3  -> "还剩 ${daysLeft} 天"
        daysLeft in 2..3 -> "还剩 ${daysLeft} 天"
        daysLeft == 1 -> "明天到期"
        daysLeft == 0 -> "今天到期"
        daysLeft < 0  -> "已过期 ${-daysLeft} 天"
        else          -> "还剩 ${daysLeft} 天"
    }

    val textColor = when (urgency) {
        UrgencyState.CALM    -> Color.Gray
        UrgencyState.NOTICE  -> urgency.color()
        UrgencyState.URGENT  -> urgency.color()
        UrgencyState.CRITICAL -> urgency.color()
    }

    Text(
        text = text,
        modifier = modifier,
        color = textColor,
        fontSize = 13.sp,
        fontWeight = if (urgency == UrgencyState.CRITICAL) FontWeight.Bold else FontWeight.Normal
    )
}