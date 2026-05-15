package com.mason.reminder.ui.screen

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.color
import com.mason.reminder.data.model.label
import com.mason.reminder.ui.component.COLOR_OPTIONS
import com.mason.reminder.ui.component.CategoryCard
import com.mason.reminder.ui.component.ICON_OPTIONS
import com.mason.reminder.ui.component.UrgencyBadge
import com.mason.reminder.ui.component.iconFromName
import com.mason.reminder.ui.component.parseColorHex
import kotlinx.coroutines.launch

/**
 * 首页 — 分类卡片列表 + 全局紧急度色块横幅 + 长按编辑删除 + FAB 新建 + 空状态提示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTasks: (categoryId: Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val categoriesWithInfo by viewModel.categoriesWithInfo.collectAsStateWithLifecycle()
    val globalUrgency by viewModel.globalUrgency.collectAsStateWithLifecycle()

    var showCreateSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf<Category?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "提醒助手",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            GlobalUrgencyChip(urgency = globalUrgency)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "设置"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                // 全局紧急度色块横幅 — TopAppBar 下方醒目色条
                GlobalUrgencyBanner(urgency = globalUrgency)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "新建分类")
            }
        }
    ) { innerPadding ->
        if (categoriesWithInfo.isEmpty()) {
            EmptyCategoryState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = categoriesWithInfo,
                    key = { it.category.id }
                ) { info ->
                    CategoryCard(
                        category = info.category,
                        taskCount = info.taskCount,
                        daysLeft = info.mostUrgentDaysLeft,
                        categoryUrgency = info.categoryUrgency,
                        onClick = { onNavigateToTasks(info.category.id) },
                        onLongClick = { showEditSheet = info.category }
                    )
                }
                // FAB clearance spacer
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // ── 新建分类底部弹窗 ──────────────────────────
    if (showCreateSheet) {
        CategoryFormSheet(
            title = "新建分类",
            initialName = "",
            initialIcon = "folder",
            initialColor = "#2E8B57",
            onConfirm = { name, icon, color ->
                viewModel.insertCategory(name, icon, color)
                showCreateSheet = false
            },
            onDismiss = { showCreateSheet = false }
        )
    }

    // ── 编辑分类底部弹窗 ──────────────────────────
    showEditSheet?.let { category ->
        CategoryFormSheet(
            title = "编辑分类",
            initialName = category.name,
            initialIcon = category.iconName,
            initialColor = category.colorHex,
            showDelete = true,
            onConfirm = { name, icon, color ->
                viewModel.updateCategory(category, name, icon, color)
                showEditSheet = null
            },
            onDismiss = { showEditSheet = null },
            onDelete = {
                showEditSheet = null
                showDeleteConfirm = category
            }
        )
    }

    // ── 删除确认对话框 ────────────────────────────
    showDeleteConfirm?.let { category ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "删除分类",
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定删除「${category.name}」？\n该分类下的所有任务也将被删除。",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 全局紧急度色块横幅 — TopAppBar 下方的醒目色条 + 状态文字。
 *
 * CALM 时几乎不可见（半透明绿色极细条），
 * NOTICE/URGENT/CRITICAL 时逐渐变宽变醒目。
 */
@Composable
private fun GlobalUrgencyBanner(urgency: UrgencyState, modifier: Modifier = Modifier) {
    val bannerColor by animateColorAsState(
        targetValue = urgency.color(),
        label = "bannerColor"
    )
    val bannerAlpha = when (urgency) {
        UrgencyState.CALM     -> 0.08f
        UrgencyState.NOTICE   -> 0.18f
        UrgencyState.URGENT   -> 0.30f
        UrgencyState.CRITICAL -> 0.45f
    }
    val bannerHeight = when (urgency) {
        UrgencyState.CALM     -> 3.dp
        UrgencyState.NOTICE   -> 4.dp
        UrgencyState.URGENT   -> 6.dp
        UrgencyState.CRITICAL -> 8.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .background(bannerColor.copy(alpha = bannerAlpha))
    )
}

/**
 * 全局紧急度芯片 — 显示在 TopAppBar 标题旁的小型指示器。
 */
@Composable
private fun GlobalUrgencyChip(urgency: UrgencyState, modifier: Modifier = Modifier) {
    val chipColor by animateColorAsState(
        targetValue = urgency.color(),
        label = "chipColor"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        UrgencyBadge(urgency = urgency, size = 10.dp)
        Text(
            text = urgency.label(),
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 空分类状态 — 引导用户添加分类的居中提示。
 */
@Composable
private fun EmptyCategoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Text(
                text = "暂无分类",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "点击右下角 + 按钮添加分类",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 分类表单底部弹窗 — 支持新建和编辑。
 * 包含名称输入、图标选择网格和颜色选择行。
 * 编辑模式下额外显示删除按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFormSheet(
    title: String,
    initialName: String,
    initialIcon: String,
    initialColor: String,
    showDelete: Boolean = false,
    onConfirm: (name: String, icon: String, color: String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题行 + 可选删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (showDelete && onDelete != null) {
                    IconButton(onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                            onDelete()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 名称输入
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分类名称") },
                placeholder = { Text("例如：工作、学习、生活") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 图标选择网格
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "图标",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconPickerGrid(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )
            }

            // 颜色选择行
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorPickerRow(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }

            // 操作按钮行
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
                        if (name.isNotBlank()) {
                            onConfirm(name.trim(), selectedIcon, selectedColor)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("确定") }
            }
        }
    }
}

/**
 * 图标选择网格 — 预设 16 个图标，选中项带 primaryContainer 圆形背景高亮。
 */
@Composable
private fun IconPickerGrid(
    selectedIcon: String,
    onIconSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ICON_OPTIONS.forEach { iconName ->
            val isSelected = iconName == selectedIcon
            IconButton(
                onClick = { onIconSelected(iconName) },
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isSelected) Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                        else Modifier
                    )
            ) {
                Icon(
                    imageVector = iconFromName(iconName),
                    contentDescription = iconName,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 颜色选择行 — 预设 12 个颜色圆点，选中项带颜色背景高亮环。
 */
@Composable
private fun ColorPickerRow(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        COLOR_OPTIONS.forEach { colorHex ->
            val parsedColor = parseColorHex(colorHex)
            val isSelected = colorHex == selectedColor
            IconButton(
                onClick = { onColorSelected(colorHex) },
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isSelected) Modifier
                            .clip(CircleShape)
                            .background(parsedColor.copy(alpha = 0.15f))
                        else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                )
            }
        }
    }
}