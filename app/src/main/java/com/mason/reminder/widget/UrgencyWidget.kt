package com.mason.reminder.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.mason.reminder.MainActivity
import com.mason.reminder.R
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.toLocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 桌面 Widget：显示全局紧急度颜色 + 最近 5 条任务 + 快捷完成按钮。
 *
 * 外部触发更新：
 * - ReminderReceiver / ReminderWorker 通过 ACTION_UPDATE 广播刷新
 * - 系统通过 updatePeriodMillis 定时刷新
 * - 用户点击 ✓ 通过 ACTION_MARK_COMPLETE 标记完成后刷新
 */
class UrgencyWidget : AppWidgetProvider() {

    companion object {
        /** ReminderReceiver / ReminderWorker 发送广播时引用此常量 */
        const val ACTION_UPDATE = "com.mason.reminder.ACTION_UPDATE_WIDGET"
        const val EXTRA_URGENCY = "com.mason.reminder.EXTRA_URGENCY"
        const val ACTION_MARK_COMPLETE = "com.mason.reminder.ACTION_MARK_COMPLETE"
        const val ACTION_OPEN_TASK = "com.mason.reminder.ACTION_OPEN_TASK"
        const val ACTION_NEW_TASK = "com.mason.reminder.ACTION_NEW_TASK"
        const val EXTRA_TASK_ID = "com.mason.reminder.EXTRA_WIDGET_TASK_ID"
        const val MAX_TASKS = 5
        private const val COMPLETE_REQUEST_OFFSET = 2000
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val allActiveEntities = db.taskDao().getAllActiveOnce()
            val tasks = allActiveEntities.map { it.toDomain() }
            val now = LocalDate.now()
            val globalUrgency = UrgencyCalculator.maxUrgency(tasks, now)

            // 按紧急度降序 + 到期日期升序排列，取前 5 条
            val topTasks = tasks.sortedWith(
                compareByDescending<Task> { UrgencyCalculator.calculate(it, now).ordinal }
                    .thenBy { it.nextDueDate }
            ).take(MAX_TASKS)

            for (appWidgetId in appWidgetIds) {
                val views = buildRemoteViews(context, appWidgetId, globalUrgency, topTasks, now)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // 第一个 Widget 实例添加到桌面时，启动每日颜色更新闹钟
        IconUpdater.scheduleDailyUpdate(context)
    }

    override fun onDisabled(context: Context) {
        // 最后一个 Widget 实例从桌面移除时，取消每日颜色更新闹钟
        IconUpdater.cancelDailyUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_UPDATE -> refreshAllWidgets(context)
            ACTION_MARK_COMPLETE -> handleMarkComplete(context, intent)
            else -> super.onReceive(context, intent)
        }
    }

    // ── 内部方法 ──────────────────────────────────────────

    private fun refreshAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, UrgencyWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun handleMarkComplete(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            AppDatabase.getInstance(context).taskDao().markCompleted(taskId, now)
            refreshAllWidgets(context)
        }
    }

    /**
     * 构建 Widget 的 RemoteViews。
     *
     * - 根背景 → 全局紧急度对应的圆角 drawable
     * - 标题 → "提醒助手 · {紧急度标签}" + 紧急度色块
     * - 0~4 行 → 色块 + 标题 + 倒计时 + ✓按钮
     * - 空状态 → "一切正常，无待办到期"
     * - 底部 → "新建任务" 按钮
     */
    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        globalUrgency: UrgencyState,
        tasks: List<Task>,
        now: LocalDate
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.urgency_widget_layout)

        // ── 根背景（圆角 + 紧急度颜色）──
        val bgResId = when (globalUrgency) {
            UrgencyState.CALM    -> R.drawable.widget_bg_calm
            UrgencyState.NOTICE  -> R.drawable.widget_bg_notice
            UrgencyState.URGENT  -> R.drawable.widget_bg_urgent
            UrgencyState.CRITICAL -> R.drawable.widget_bg_critical
        }
        views.setInt(R.id.widget_root, "setBackgroundResource", bgResId)

        // ── 标题色块 ──
        views.setInt(R.id.widget_urgency_icon, "setBackgroundColor", WidgetColorMapper.colorFor(globalUrgency))

        // ── 标题文字 ──
        views.setTextViewText(R.id.widget_title, "提醒助手 · ${globalUrgency.label()}")

        // ── 任务列表 vs 空状态 ──
        if (tasks.isEmpty()) {
            views.setViewVisibility(R.id.widget_task_list, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_task_list, View.VISIBLE)
            views.setViewVisibility(R.id.widget_empty, View.GONE)

            for (i in 0 until MAX_TASKS) {
                val rowId = rowIdFor(i)
                if (i < tasks.size) {
                    val task = tasks[i]
                    val urgency = UrgencyCalculator.calculate(task, now)
                    views.setViewVisibility(rowId, View.VISIBLE)

                    // 色块
                    views.setInt(colorIdFor(i), "setBackgroundColor", WidgetColorMapper.colorFor(urgency))

                    // 标题
                    views.setTextViewText(titleIdFor(i), task.title)

                    // 倒计时
                    views.setTextViewText(countdownIdFor(i), formatCountdown(task, now))

                    // 倒计时文字颜色：紧急度高时白色高对比
                    val countdownColor = if (urgency.ordinal >= UrgencyState.URGENT.ordinal) {
                        0xFFFFFFFF.toInt()
                    } else {
                        0xCCFFFFFF.toInt()
                    }
                    views.setInt(countdownIdFor(i), "setTextColor", countdownColor)

                    // 点击任务行 → 打开 TaskDetailScreen(taskId)
                    val openTaskIntent = Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_TASK
                        putExtra(EXTRA_TASK_ID, task.id)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        task.id.toInt(),
                        openTaskIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(rowId, openPendingIntent)

                    // ✓ 按钮 → 广播标记完成
                    val completeIntent = Intent(context, UrgencyWidget::class.java).apply {
                        action = ACTION_MARK_COMPLETE
                        putExtra(EXTRA_TASK_ID, task.id)
                    }
                    val completePendingIntent = PendingIntent.getBroadcast(
                        context,
                        COMPLETE_REQUEST_OFFSET + task.id.toInt(),
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(completeIdFor(i), completePendingIntent)
                } else {
                    views.setViewVisibility(rowId, View.GONE)
                }
            }
        }

        // ── 新建任务按钮 ──
        val newTaskIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_NEW_TASK
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val newPendingIntent = PendingIntent.getActivity(
            context,
            0,
            newTaskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_new_btn, newPendingIntent)

        // ── 标题行点击 → 打开 MainActivity ──
        val titleIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        views.setOnClickPendingIntent(
            R.id.widget_title_bar,
            PendingIntent.getActivity(
                context,
                -1,
                titleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return views
    }

    // ── 倒计时格式 ──

    private fun formatCountdown(task: Task, now: LocalDate): String {
        val dueDate = task.nextDueDate.toLocalDate()
        val daysLeft = dueDate.toEpochDay() - now.toEpochDay()
        return when {
            daysLeft < 0  -> "过期${-daysLeft}天"
            daysLeft == 0 -> "今天到期"
            daysLeft == 1 -> "明天到期"
            else          -> "还剩${daysLeft}天"
        }
    }

    // ── View ID 辅助 ──

    private fun rowIdFor(index: Int): Int = when (index) {
        0 -> R.id.widget_task_row_0
        1 -> R.id.widget_task_row_1
        2 -> R.id.widget_task_row_2
        3 -> R.id.widget_task_row_3
        4 -> R.id.widget_task_row_4
        else -> View.NO_ID
    }

    private fun colorIdFor(index: Int): Int = when (index) {
        0 -> R.id.widget_task_color_0
        1 -> R.id.widget_task_color_1
        2 -> R.id.widget_task_color_2
        3 -> R.id.widget_task_color_3
        4 -> R.id.widget_task_color_4
        else -> View.NO_ID
    }

    private fun titleIdFor(index: Int): Int = when (index) {
        0 -> R.id.widget_task_title_0
        1 -> R.id.widget_task_title_1
        2 -> R.id.widget_task_title_2
        3 -> R.id.widget_task_title_3
        4 -> R.id.widget_task_title_4
        else -> View.NO_ID
    }

    private fun countdownIdFor(index: Int): Int = when (index) {
        0 -> R.id.widget_task_countdown_0
        1 -> R.id.widget_task_countdown_1
        2 -> R.id.widget_task_countdown_2
        3 -> R.id.widget_task_countdown_3
        4 -> R.id.widget_task_countdown_4
        else -> View.NO_ID
    }

    private fun completeIdFor(index: Int): Int = when (index) {
        0 -> R.id.widget_task_complete_0
        1 -> R.id.widget_task_complete_1
        2 -> R.id.widget_task_complete_2
        3 -> R.id.widget_task_complete_3
        4 -> R.id.widget_task_complete_4
        else -> View.NO_ID
    }
}