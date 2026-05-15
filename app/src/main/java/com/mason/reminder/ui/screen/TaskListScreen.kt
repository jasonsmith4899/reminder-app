package com.mason.reminder.ui.screen

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.ui.component.TaskCard
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 任务列表页 — 任务卡片列表 + UrgencyBadge + 倒计时文字 + 循环间隔显示
 * + 批量操作（选择/完成/删除/全选） + 排序切换（紧急度/到期日）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (taskId: Long) -> Unit,
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val tasksWithUrgency by viewModel.tasksWithUrgency.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedTasks by viewModel.selectedTasks.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = category?.name ?: "待办列表",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSelectionMode) viewModel.clearSelection() else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isSelectionMode) {
                        // 排序切换 FilterChip
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.toggleSortMode() },
                            label = {
                                Text(
                                    text = when (sortMode) {
                                        SortMode.BY_URGENCY -> "按紧急度"
                                        SortMode.BY_DUE_DATE -> "按到期日"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SortByAlpha,
                                    contentDescription = "排序",
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新建待办")
                }
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                BottomAppBar {
                    // 全选按钮
                    IconButton(onClick = { viewModel.selectAll() }) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "全选"
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    // 已选数量
                    Text(
                        text = "已选 ${selectedTasks.size} 项",
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 批量完成
                    IconButton(onClick = { viewModel.completeSelectedTasks() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "批量完成",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 批量删除
                    IconButton(onClick = { viewModel.deleteSelectedTasks() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "批量删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (tasksWithUrgency.isEmpty()) {
            EmptyTaskState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = tasksWithUrgency,
                    key = { it.task.id }
                ) { taskWithUrgency ->
                    TaskCard(
                        task = taskWithUrgency.task,
                        urgency = taskWithUrgency.urgency,
                        daysLeft = taskWithUrgency.daysLeft,
                        isSelected = taskWithUrgency.task.id in selectedTasks,
                        isSelectionMode = isSelectionMode,
                        onClick = { onNavigateToDetail(taskWithUrgency.task.id) },
                        onLongClick = { viewModel.toggleTaskSelection(taskWithUrgency.task.id) },
                        onToggleSelection = { viewModel.toggleTaskSelection(taskWithUrgency.task.id) }
                    )
                }
                // FAB clearance
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ── 新建待办底部弹窗 ─────────────────────────
    if (showAddSheet) {
        AddTaskSheet(
            onConfirm = { title, type, level, dueDate, intervalValue, intervalUnit ->
                viewModel.insertTask(title, type, level, dueDate, intervalValue, intervalUnit)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

/**
 * 空待办状态 — 居中提示用户添加待办。
 */
@Composable
private fun EmptyTaskState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "暂无待办",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "点击右下角 + 按钮添加待办",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 新建待办底部弹窗 — 标题输入 + 提醒类型切换 + 提醒级别选择 + 日期选择 + 循环间隔配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
    onConfirm: (
        title: String,
        reminderType: ReminderType,
        reminderLevel: ReminderLevel,
        dueDate: LocalDate?,
        intervalValue: Int?,
        intervalUnit: IntervalUnit?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var reminderType by remember { mutableStateOf(ReminderType.ONCE) }
    var reminderLevel by remember { mutableStateOf(ReminderLevel.MEDIUM) }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var intervalValue by remember { mutableIntStateOf(1) }
    var intervalUnit by remember { mutableStateOf(IntervalUnit.DAY) }

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 标题
            Text(
                text = "新建待办",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // 名称输入
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                placeholder = { Text("例如：提交季度报告") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 提醒类型切换
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "提醒类型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { reminderType = ReminderType.ONCE },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (reminderType == ReminderType.ONCE)
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        else
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) { Text("一次性") }
                    OutlinedButton(
                        onClick = { reminderType = ReminderType.RECURRING },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (reminderType == ReminderType.RECURRING)
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        else
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    ) { Text("循环") }
                }
            }

            // 提醒级别选择
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "提醒级别",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ReminderLevel.entries.forEach { level ->
                        val label = when (level) {
                            ReminderLevel.LIGHT  -> "轻"
                            ReminderLevel.MEDIUM -> "中"
                            ReminderLevel.HEAVY  -> "重"
                            ReminderLevel.CUSTOM -> "自"
                        }
                        FilterChip(
                            selected = reminderLevel == level,
                            onClick = { reminderLevel = level },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 日期选择
            val dateLabel = dueDate?.let { "${it.monthValue}月${it.dayOfMonth}日" } ?: "选择日期"
            OutlinedTextField(
                value = dateLabel,
                onValueChange = {},
                readOnly = true,
                label = {
                    Text(if (reminderType == ReminderType.ONCE) "到期日期" else "开始日期")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val initial = dueDate ?: LocalDate.now()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                dueDate = LocalDate.of(year, month + 1, day)
                            },
                            initial.year,
                            initial.monthValue - 1,
                            initial.dayOfMonth
                        ).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "选择日期"
                        )
                    }
                }
            )

            // 循环间隔配置（仅在循环模式下显示）
            if (reminderType == ReminderType.RECURRING) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "循环间隔",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = intervalValue.toString(),
                            onValueChange = { intervalValue = it.toIntOrNull() ?: 1 },
                            label = { Text("间隔") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IntervalUnit.entries.forEach { unit ->
                                val label = when (unit) {
                                    IntervalUnit.DAY   -> "天"
                                    IntervalUnit.WEEK  -> "周"
                                    IntervalUnit.MONTH -> "月"
                                }
                                FilterChip(
                                    selected = intervalUnit == unit,
                                    onClick = { intervalUnit = unit },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("取消") }
                FilledTonalButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onConfirm(
                                title.trim(),
                                reminderType,
                                reminderLevel,
                                dueDate,
                                if (reminderType == ReminderType.RECURRING) intervalValue else null,
                                if (reminderType == ReminderType.RECURRING) intervalUnit else null
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("确定") }
            }
        }
    }
}