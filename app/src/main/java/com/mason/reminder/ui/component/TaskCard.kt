package com.mason.reminder.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.color

/**
 * 任务卡片组件 — 展示紧急度色块、标题、倒计时、提醒级别和循环间隔。
 *
 * 支持选择模式（多选 Checkbox）和普通模式（UrgencyBadge + 点击进入详情）。
 * 长按触发多选模式。Card elevation/背景色随紧急度和选中状态动态变化。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    urgency: UrgencyState,
    daysLeft: Long,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgencyColor by animateColorAsState(
        targetValue = urgency.color(),
        label = "urgencyColor"
    )

    val cardElevation by animateDpAsState(
        targetValue = when {
            isSelected -> 6.dp
            urgency >= UrgencyState.URGENT -> 3.dp
            else -> 1.dp
        },
        label = "cardElevation"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            urgency == UrgencyState.CRITICAL -> urgencyColor.copy(alpha = 0.04f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = if (isSelectionMode) onToggleSelection else onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 选择模式下显示 Checkbox，普通模式下显示 UrgencyBadge
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                UrgencyBadge(
                    urgency = urgency,
                    size = 14.dp,
                    modifier = Modifier.padding(end = 10.dp)
                )
            }

            // 内容列：标题 + 元信息行
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (urgency == UrgencyState.CRITICAL)
                        FontWeight.Bold else FontWeight.Medium,
                    textDecoration = if (!task.isActive) TextDecoration.LineThrough
                    else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (!task.isActive)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 元信息行：倒计时 · 提醒级别 · 循环间隔
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CountdownText(daysLeft = daysLeft, urgency = urgency)

                    // 圆点分隔符
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                    )

                    ReminderLevelChip(level = task.reminderLevel)

                    // 循环任务显示间隔
                    if (task.reminderType == ReminderType.RECURRING) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                        )
                        RecurringIntervalBadge(
                            intervalValue = task.intervalValue,
                            intervalUnit = task.intervalUnit
                        )
                    }
                }
            }
        }
    }
}

/**
 * 提醒级别小标签 — 轻/中/重/自定义，带对应颜色圆角背景。
 */
@Composable
private fun ReminderLevelChip(level: ReminderLevel, modifier: Modifier = Modifier) {
    val (label, chipColor) = when (level) {
        ReminderLevel.LIGHT  -> "轻" to MaterialTheme.colorScheme.tertiary
        ReminderLevel.MEDIUM -> "中" to MaterialTheme.colorScheme.primary
        ReminderLevel.HEAVY  -> "重" to MaterialTheme.colorScheme.error
        ReminderLevel.CUSTOM -> "自定义" to MaterialTheme.colorScheme.secondary
    }

    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = chipColor,
        fontWeight = FontWeight.Medium
    )
}

/**
 * 循环间隔标签 — 显示 "每 N 天/周/月"，带 Loop 图标。
 */
@Composable
private fun RecurringIntervalBadge(
    intervalValue: Int?,
    intervalUnit: IntervalUnit?,
    modifier: Modifier = Modifier
) {
    if (intervalValue == null || intervalUnit == null) return

    val unitLabel = when (intervalUnit) {
        IntervalUnit.DAY   -> "天"
        IntervalUnit.WEEK  -> "周"
        IntervalUnit.MONTH -> "月"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Loop,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Text(
            text = "每 $intervalValue $unitLabel",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}