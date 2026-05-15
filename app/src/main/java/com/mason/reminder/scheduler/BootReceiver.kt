package com.mason.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.ReminderCalculator
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.daysUntil
import com.mason.reminder.util.toLocalDate
import com.mason.reminder.widget.IconUpdater
import com.mason.reminder.widget.UrgencyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 处理 BOOT_COMPLETED 广播。
 *
 * 设备重启后，AlarmManager 注册的闹钟全部丢失。
 * 此 Receiver 查询所有活跃任务，重新调度闹钟。
 *
 * 小米/澎湃OS 默认禁止自启动，需引导用户开启自启动权限。
 * 即使自启动被禁，WorkManager 兜底 Worker 仍可在省电策略允许时触发。
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = "BootReceiver"
        private val DEFAULT_NOTIFY_TIME = LocalTime.of(9, 0)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "BOOT_COMPLETED received, rescheduling all alarms")

        // goAsync() 给予 ~10s 保护窗口，防止 onReceive 返回后进程被杀
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllAlarms(context)
                // 调度每日 Widget 颜色更新闹钟
                IconUpdater.scheduleDailyUpdate(context)
                // 同时调度 WorkManager 每日兜底检查
                scheduleDailyFallback(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 查询所有活跃任务，为每个需要通知的任务重新调度闹钟。
     *
     * 规则：
     * - 轻度：只在到期日当天 09:00 调度
     * - 中度：到期日前 3 天起，每天 09:00 调度
     * - 重度：到期日前 15 天起，每天 09:00 调度
     * - 自定义：按 customAdvanceDays 计算
     * - 已过期但未完成的任务：立即通知
     */
    private suspend fun rescheduleAllAlarms(context: Context) {
        val db = AppDatabase.getInstance(context)
        val allActiveEntities = db.taskDao().getAllActiveOnce()
        val allActiveTasks = allActiveEntities.map { it.toDomain() }
        val now = LocalDate.now()

        for (task in allActiveTasks) {
            val dueDate = task.nextDueDate.toLocalDate()
            val daysLeft = dueDate.daysUntil(now).toInt()

            val notifyTime = calculateNotifyTime(task, daysLeft, now)

            if (notifyTime != null) {
                AlarmScheduler.setExactAlarm(context, task.id, notifyTime)
                Log.d(TAG, "Rescheduled alarm for task ${task.id} at $notifyTime")
            } else if (daysLeft <= 0) {
                // 已过期：立即通知
                val urgency = UrgencyCalculator.calculate(task, now)
                NotificationHelper.showNotification(
                    context = context,
                    taskId = task.id,
                    title = task.title,
                    level = task.reminderLevel,
                    urgency = urgency,
                    daysLeft = 0,
                    isDueToday = true
                )
                Log.d(TAG, "Task ${task.id} is overdue, showing notification immediately")
            }
        }

        // 更新 Widget
        val maxUrgency = UrgencyCalculator.maxUrgency(allActiveTasks, now)
        val widgetIntent = Intent(context, UrgencyWidget::class.java).apply {
            action = UrgencyWidget.ACTION_UPDATE
            putExtra(UrgencyWidget.EXTRA_URGENCY, maxUrgency.name)
        }
        context.sendBroadcast(widgetIntent)
    }

    /**
     * 计算当前应该调度闹钟的时间。
     *
     * 如果今天已经过了通知时间点，则调度到明天 09:00。
     * 如果还没到提醒窗口，则计算首次提醒时间。
     */
    private fun calculateNotifyTime(
        task: com.mason.reminder.data.model.Task,
        daysLeft: Int,
        today: LocalDate
    ): LocalDateTime? {
        // 判断当前是否处于提醒窗口内
        val shouldNotifyNow = when (task.reminderLevel) {
            ReminderLevel.LIGHT -> daysLeft <= 0
            ReminderLevel.MEDIUM -> daysLeft <= 3
            ReminderLevel.HEAVY -> daysLeft <= 15
            ReminderLevel.CUSTOM -> daysLeft <= (task.customAdvanceDays ?: 3)
        }

        if (!shouldNotifyNow) {
            // 还没到提醒窗口，计算首次提醒时间
            val firstNotifyTime = ReminderCalculator.firstNotifyTime(
                dueDate = task.nextDueDate.toLocalDate(),
                level = task.reminderLevel,
                customAdvanceDays = task.customAdvanceDays
            )
            // 如果首次提醒时间在过去（可能已被错过），调度到最近的通知时间
            if (firstNotifyTime.isBefore(LocalDateTime.now())) {
                val todayNotifyTime = LocalDateTime.of(today, DEFAULT_NOTIFY_TIME)
                return if (LocalDateTime.now().isBefore(todayNotifyTime)) {
                    todayNotifyTime
                } else {
                    LocalDateTime.of(today.plusDays(1), DEFAULT_NOTIFY_TIME)
                }
            }
            return firstNotifyTime
        }

        // 已在提醒窗口内，调度到今天或明天 09:00
        val nowTime = LocalDateTime.now()
        val todayNotifyTime = LocalDateTime.of(today, DEFAULT_NOTIFY_TIME)

        return if (nowTime.isBefore(todayNotifyTime)) {
            todayNotifyTime // 今天还没到 09:00
        } else {
            LocalDateTime.of(today.plusDays(1), DEFAULT_NOTIFY_TIME) // 今天已过 09:00
        }
    }

    /**
     * 调度 WorkManager 每日兜底检查。
     *
     * 作为 AlarmManager 的补充，防止省电策略杀闹钟。
     * 每 24 小时执行一次，检查所有活跃任务是否需要补发通知。
     */
    private fun scheduleDailyFallback(context: Context) {
        val dailyWork = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS,
            30, TimeUnit.MINUTES // flex interval: 可在 24h ± 30min 内触发
        )
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(ReminderWorker.KEY_DAILY_CHECK, true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                ReminderWorker.WORK_NAME_DAILY,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP, // 已存在则不替换
                dailyWork
            )

        Log.d(TAG, "Scheduled daily fallback WorkManager check")
    }
}