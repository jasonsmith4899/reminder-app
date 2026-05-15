package com.mason.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.toLocalDate
import com.mason.reminder.widget.UrgencyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * BroadcastReceiver，处理 AlarmManager 闹钟触发。
 *
 * 核心职责：
 * 1. 根据 taskId 查询任务
 * 2. 调用 NotificationHelper 发送/更新通知
 * 3. 如果还有后续提醒日（中度/重度/自定义倒计时），调度下一个闹钟
 * 4. 到期日当天不再调度后续提醒，等用户操作
 * 5. 更新 Widget 颜色（全局紧急度）
 * 6. 记录 reminder_log
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "com.mason.reminder.ACTION_REMINDER"
        const val EXTRA_TASK_ID = "com.mason.reminder.EXTRA_TASK_ID"
        private val TAG = "ReminderReceiver"
        private val DEFAULT_NOTIFY_TIME = LocalTime.of(9, 0)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) {
            Log.w(TAG, "Invalid taskId in intent")
            return
        }

        Log.d(TAG, "Alarm triggered for task $taskId")

        // goAsync() 给予 ~10s 保护窗口，防止 onReceive 返回后进程被杀
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminder(context, taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReminder(context: Context, taskId: Long) {
        val db = AppDatabase.getInstance(context)
        val taskEntity = db.taskDao().getByIdOnce(taskId)

        if (taskEntity == null || !taskEntity.isActive) {
            // 任务已删除或不活跃，取消闹钟和通知
            Log.d(TAG, "Task $taskId not found or inactive, cancelling alarm")
            AlarmScheduler.cancelAlarm(context, taskId)
            NotificationHelper.cancelNotification(context, taskId)
            return
        }

        val task = taskEntity.toDomain()
        val now = LocalDate.now()
        val urgency = UrgencyCalculator.calculate(task, now)

        val dueDate = task.nextDueDate.toLocalDate()
        val daysLeft = dueDate.daysUntil(now).toInt()
        val isDueToday = daysLeft <= 0

        Log.d(TAG, "Task $taskId: urgency=$urgency, daysLeft=$daysLeft, isDueToday=$isDueToday")

        // 1. 发送/更新通知
        NotificationHelper.showNotification(
            context = context,
            taskId = task.id,
            title = task.title,
            level = task.reminderLevel,
            urgency = urgency,
            daysLeft = daysLeft,
            isDueToday = isDueToday
        )

        // 2. 记录通知日志
        db.reminderLogDao().insert(
            com.mason.reminder.data.db.entity.ReminderLogEntity(
                taskId = task.id,
                notifiedAt = System.currentTimeMillis(),
                daysBeforeDue = maxOf(daysLeft, 0),
                actionTaken = "NOTIFIED"
            )
        )

        // 3. 调度下一个倒计时通知（仅中度/重度/自定义且非到期日）
        if (!isDueToday && task.reminderLevel != ReminderLevel.LIGHT) {
            val nextNotifyTime = calculateNextNotifyTime(task, daysLeft)
            if (nextNotifyTime != null) {
                AlarmScheduler.setExactAlarm(context, task.id, nextNotifyTime)
                Log.d(TAG, "Scheduled next alarm for task $taskId at $nextNotifyTime")
            }
        }

        // 4. 更新 Widget 颜色（触发全局紧急度刷新）
        val allActiveEntities = db.taskDao().getAllActiveOnce()
        val allActiveTasks = allActiveEntities.map { it.toDomain() }
        val maxUrgency = UrgencyCalculator.maxUrgency(allActiveTasks, now)
        updateWidget(context, maxUrgency)
    }

    /**
     * 计算下一次倒计时通知时间。
     *
     * 规则：
     * - 轻度：只在到期日当天通知一次，不需要后续
     * - 中度/重度/自定义：每天 09:00 更新通知直到到期日
     * - 如果距到期日只剩 1 天，下次通知时间改为到期日当天早上
     */
    private fun calculateNextNotifyTime(
        task: com.mason.reminder.data.model.Task,
        daysLeft: Int
    ): LocalDateTime? {
        val nextDay = LocalDate.now().plusDays(1)
        val nextNotifyTime = LocalDateTime.of(nextDay, DEFAULT_NOTIFY_TIME)

        val dueDate = task.nextDueDate.toLocalDate()
        // 如果明天就是到期日，也在到期日当天通知一次（这次会变成"今天到期！"）
        if (nextDay >= dueDate) {
            return LocalDateTime.of(dueDate, DEFAULT_NOTIFY_TIME)
        }

        return nextNotifyTime
    }

    /**
     * 通过广播更新 Widget 颜色。
     */
    private fun updateWidget(context: Context, urgency: com.mason.reminder.data.model.UrgencyState) {
        val intent = Intent(context, UrgencyWidget::class.java).apply {
            action = UrgencyWidget.ACTION_UPDATE
            putExtra(UrgencyWidget.EXTRA_URGENCY, urgency.name)
        }
        context.sendBroadcast(intent)
    }
}