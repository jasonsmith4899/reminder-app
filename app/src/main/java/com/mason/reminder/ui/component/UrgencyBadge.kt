package com.mason.reminder.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.color

/**
 * 紧急度色块组件：圆形色点，颜色由 [urgency] 决定。
 */
@Composable
fun UrgencyBadge(
    urgency: UrgencyState,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(urgency.color())
    )
}