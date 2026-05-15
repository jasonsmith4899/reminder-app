package com.mason.reminder.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SnoozeActionRow(
    onSnooze1h: () -> Unit,
    onSnooze3h: () -> Unit,
    onSnoozeTomorrow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSnooze1h,
            modifier = Modifier.weight(1f)
        ) {
            Text("1小时")
        }
        OutlinedButton(
            onClick = onSnooze3h,
            modifier = Modifier.weight(1f)
        ) {
            Text("3小时")
        }
        OutlinedButton(
            onClick = onSnoozeTomorrow,
            modifier = Modifier.weight(1f)
        ) {
            Text("明天")
        }
    }
}