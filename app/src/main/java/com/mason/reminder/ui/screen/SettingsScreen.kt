package com.mason.reminder.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBackupClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAutoStartDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── 默认提醒时间 ──────────────────────────────
            SettingsCard(title = "默认提醒时间", icon = Icons.Default.Alarm) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${uiState.defaultNotifyHour}:${uiState.defaultNotifyMinute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedButton(onClick = { showTimePickerDialog = true }) {
                        Text("修改时间")
                    }
                }
                Text(
                    text = "新建任务时默认使用此时间发送提醒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── 通知偏好 ──────────────────────────────────
            SettingsCard(title = "通知偏好", icon = Icons.Default.Notifications) {
                // 振动开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Vibration,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("振动提醒")
                    }
                    Switch(
                        checked = uiState.vibrationEnabled,
                        onCheckedChange = viewModel::setVibrationEnabled
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 打开通知设置按钮
                OutlinedButton(
                    onClick = viewModel::openNotificationSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开系统通知设置")
                }
            }

            // ── 精确闹钟 ──────────────────────────────────
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SettingsCard(title = "精确闹钟", icon = Icons.Default.Alarm) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (uiState.exactAlarmEnabled) "已授权" else "未授权（降级不精确闹钟）",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.exactAlarmEnabled)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        if (!uiState.exactAlarmEnabled) {
                            OutlinedButton(onClick = viewModel::openExactAlarmSettings) {
                                Text("授权")
                            }
                        }
                    }
                    Text(
                        text = "Android 12+ 需要精确闹钟权限才能准时提醒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── 自启动提示 ──────────────────────────────────
            if (uiState.autoStartHintVisible) {
                SettingsCard(title = "自启动", icon = Icons.Default.PowerSettingsNew) {
                    Text(
                        text = "部分系统（小米、华为等）会限制应用自启动，导致开机后闹钟无法恢复。建议在系统设置中允许本应用自启动。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = viewModel::dismissAutoStartHint) {
                            Text("知道了")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = viewModel::openAutoStartSettings) {
                            Text("去设置")
                        }
                    }
                }
            }

            // ── 备份与恢复 ──────────────────────────────────
            SettingsCard(
                title = "备份与恢复",
                icon = Icons.Default.Backup,
                onClick = onBackupClick
            ) {
                Text(
                    text = "导出 / 导入任务数据为 JSON 文件",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── 关于 ──────────────────────────────────────
            SettingsCard(title = "关于", icon = Icons.Default.Info) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("提醒助手", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "v${uiState.appVersion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── 时间选择对话框 ──────────────────────────────
    if (showTimePickerDialog) {
        TimePickerDialog(
            currentHour = uiState.defaultNotifyHour,
            currentMinute = uiState.defaultNotifyMinute,
            onConfirm = { hour, minute ->
                viewModel.setDefaultNotifyHour(hour)
                viewModel.setDefaultNotifyMinute(minute)
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    // ── 自启动引导对话框 ──────────────────────────────
    if (showAutoStartDialog) {
        AlertDialog(
            onDismissRequest = { showAutoStartDialog = false },
            title = { Text("开启自启动") },
            text = { Text("为确保开机后闹钟正常恢复，请在系统设置中允许提醒助手自启动。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.openAutoStartSettings()
                    showAutoStartDialog = false
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoStartDialog = false }) { Text("稍后") }
            }
        )
    }
}

// ── 内部组件 ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
    val cardContent: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors,
            content = cardContent
        )
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = cardColors,
            content = cardContent
        )
    }
}

@Composable
private fun TimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(currentHour) }
    var minute by remember { mutableStateOf(currentMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择默认提醒时间") },
        text = {
            Column {
                Text("时：$hour", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 23
                )
                Spacer(Modifier.height(8.dp))
                Text("分：$minute", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = minute.toFloat(),
                    onValueChange = { minute = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 59
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}