package com.mason.reminder.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mason.reminder.data.model.IntervalUnit
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.ui.component.ReminderLevelPicker
import com.mason.reminder.util.formatDisplay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onSaved: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == 0L) "新建任务" else "编辑任务") }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 标题 ──
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("标题（必填）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = uiState.error != null && uiState.title.isBlank()
                )

                // ── 描述 ──
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // ── 分类下拉 ──
                CategoryDropdown(
                    categories = uiState.categories,
                    selectedId = uiState.categoryId,
                    onSelect = viewModel::updateCategoryId
                )

                // ── 提醒类型切换 ──
                ReminderTypeToggle(
                    type = uiState.reminderType,
                    onTypeChange = viewModel::updateReminderType
                )

                // ── 日期字段（根据类型显示） ──
                if (uiState.reminderType == ReminderType.ONCE) {
                    DatePickerField(
                        label = "到期日期",
                        date = uiState.dueDate,
                        onDateChange = viewModel::updateDueDate
                    )
                } else {
                    RecurringFields(
                        intervalValue = uiState.intervalValue,
                        intervalUnit = uiState.intervalUnit,
                        startDate = uiState.startDate,
                        onIntervalValueChange = viewModel::updateIntervalValue,
                        onIntervalUnitChange = viewModel::updateIntervalUnit,
                        onStartDateChange = viewModel::updateStartDate
                    )
                }

                // ── 提醒级别 ──
                ReminderLevelPicker(
                    level = uiState.reminderLevel,
                    customAdvanceDays = uiState.customAdvanceDays,
                    customNotifyFreq = uiState.customNotifyFreq,
                    onLevelChange = viewModel::updateReminderLevel,
                    onCustomAdvanceDaysChange = viewModel::updateCustomAdvanceDays,
                    onCustomNotifyFreqChange = viewModel::updateCustomNotifyFreq
                )

                Spacer(Modifier.height(8.dp))

                // ── 保存按钮 ──
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存")
                }
            }
        }
    }
}

// ── 分类下拉选择 ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.mason.reminder.data.model.Category>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedId }?.name ?: "选择分类"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("分类") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ── 提醒类型切换 ──

@Composable
private fun ReminderTypeToggle(
    type: ReminderType,
    onTypeChange: (ReminderType) -> Unit
) {
    Column {
        Text(
            text = "提醒类型",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == ReminderType.ONCE,
                onClick = { onTypeChange(ReminderType.ONCE) },
                label = { Text("一次性") }
            )
            FilterChip(
                selected = type == ReminderType.RECURRING,
                onClick = { onTypeChange(ReminderType.RECURRING) },
                label = { Text("循环") }
            )
        }
    }
}

// ── 日期选择器字段 ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = date?.formatDisplay() ?: "",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("请选择") },
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Event, contentDescription = "选择日期")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.toUtcMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDateChange(datePickerState.selectedDateMillis?.toLocalDateFromUtc())
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── 循环提醒字段 ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringFields(
    intervalValue: Int,
    intervalUnit: IntervalUnit,
    startDate: LocalDate?,
    onIntervalValueChange: (Int) -> Unit,
    onIntervalUnitChange: (IntervalUnit) -> Unit,
    onStartDateChange: (LocalDate?) -> Unit
) {
    Column {
        Text(
            text = "循环间隔",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = intervalValue.toString(),
                onValueChange = {
                    val num = it.toIntOrNull()
                    if (num != null && num > 0) onIntervalValueChange(num)
                },
                label = { Text("间隔数值") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(Modifier.height(8.dp))

        Text("间隔单位", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntervalUnit.entries.forEach { unit ->
                FilterChip(
                    selected = intervalUnit == unit,
                    onClick = { onIntervalUnitChange(unit) },
                    label = { Text(unitLabel(unit)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        DatePickerField(
            label = "起始日期",
            date = startDate,
            onDateChange = onStartDateChange
        )
    }
}

private fun unitLabel(unit: IntervalUnit): String = when (unit) {
    IntervalUnit.DAY   -> "天"
    IntervalUnit.WEEK  -> "周"
    IntervalUnit.MONTH -> "月"
}

// ── UTC 日期转换辅助 ──

private fun LocalDate.toUtcMillis(): Long =
    this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long?.toLocalDateFromUtc(): LocalDate? = this?.let {
    Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
}