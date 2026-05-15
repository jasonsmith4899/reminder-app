package com.mason.reminder.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mason.reminder.data.model.CustomNotifyFreq
import com.mason.reminder.data.model.ReminderLevel

@Composable
fun ReminderLevelPicker(
    level: ReminderLevel,
    customAdvanceDays: Int,
    customNotifyFreq: CustomNotifyFreq,
    onLevelChange: (ReminderLevel) -> Unit,
    onCustomAdvanceDaysChange: (Int) -> Unit,
    onCustomNotifyFreqChange: (CustomNotifyFreq) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "提醒级别",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReminderLevel.entries.forEach { l ->
                FilterChip(
                    selected = level == l,
                    onClick = { onLevelChange(l) },
                    label = { Text(levelLabel(l)) }
                )
            }
        }

        if (level == ReminderLevel.CUSTOM) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = customAdvanceDays.toString(),
                onValueChange = {
                    val num = it.toIntOrNull()
                    if (num != null && num >= 0) onCustomAdvanceDaysChange(num)
                },
                label = { Text("提前天数") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "通知频率",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomNotifyFreq.entries.forEach { freq ->
                    FilterChip(
                        selected = customNotifyFreq == freq,
                        onClick = { onCustomNotifyFreqChange(freq) },
                        label = { Text(freqLabel(freq)) }
                    )
                }
            }
        }
    }
}

private fun levelLabel(level: ReminderLevel): String = when (level) {
    ReminderLevel.LIGHT  -> "轻度"
    ReminderLevel.MEDIUM -> "中度"
    ReminderLevel.HEAVY  -> "重度"
    ReminderLevel.CUSTOM -> "自定义"
}

private fun freqLabel(freq: CustomNotifyFreq): String = when (freq) {
    CustomNotifyFreq.DAILY        -> "每天"
    CustomNotifyFreq.EVERY_2_DAYS -> "隔2天"
    CustomNotifyFreq.WEEKLY       -> "每周"
}