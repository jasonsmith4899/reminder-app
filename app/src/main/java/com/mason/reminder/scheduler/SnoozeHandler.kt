package com.mason.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.daysUntil
import com.mason.reminder.util.toLocalDate
import com.mason.reminder.widget.UrgencyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 处理通知上的"稍后提醒"（Snooze）按钮点击。
 *
 * 支持 3 种 Snooze 模式：
 * - SNOOZE_1H：1 小时后再次提醒
 * - SNOOZE_3H：3 小时后再次提醒
 * - SNOOZE_TOMORROW：明天 09:00 再次提醒
 *
 * 核心职责：
 * 1. 取消当前闹钟
 * 2. 计算新的触发时间
 * 3. 重新调度闹钟
 * 4. 取消当前通知（Snooze 期间不保留旧通知）
 * 5. 记录 reminder_log（action = SNOOZED, snoozedUntil = 新触发时间）
 */
class SnoozeHandler : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE_1H = "com.mason.reminder.ACTION_SNOOZE_1H"
        const val ACTION_SNOOZE_3H = "com.mason.reminder.ACTION_SNOOZE_3H"
        const val ACTION_SNOOZE_TOMORROW = "com.mason.reminder.ACTION_SNOOZE_TOMORROW"
        const val EXTRA_TASK_ID_SNOOZE = "com.mason.reminder.EXTRA_TASK_ID_SNOOZE"
        private val TAG = "SnoozeHandler"
        private val DEFAULT_NOTIFY_TIME = LocalTime.of(9, 0)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_SNOOZE_1H &&
            action != ACTION_SNOOZE_3H &&
            action != ACTION_SNOOZE_TOMORROW) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID_SNOOZE, -1L)
        if (taskId == -1L) {
            Log.w(TAG, "Invalid taskId in snooze action")
            return
        }

        Log.d(TAG, "Snoozing task $taskId: action=$action")

        // goAsync() 给予 ~10s 保护窗口，防止 onReceive 返回后进程被杀
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleSnooze(context, taskId, action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSnooze(context: Context, taskId: Long, action: String) {
        val db = AppDatabase.getInstance(context)
        val taskEntity = db.taskDao().getByIdOnce(taskId)

        if (taskEntity == null) {
            Log.w(TAG, "Task $taskId not found")
            AlarmScheduler.cancelAlarm(context, taskId)
            NotificationHelper.cancelNotification(context, taskId)
            return
        }

        val task = taskEntity.toDomain()

        // 计算 Snooze 后的新触发时间
        val snoozeTime = calculateSnoozeTime(action)
        val snoozeMillis = snoozeTime.atZone(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        // 记录 reminder_log：SNOOZED
        db.reminderLogDao().insert(
            com.mason.reminder.data.db.entity.ReminderLogEntity(
                taskId = task.id,
                notifiedAt = System.currentTimeMillis(),
                daysBeforeDue = task.nextDueDate.toLocalDate().daysUntil(LocalDate.now()).toInt(),
                actionTaken = "SNOOZED",
                snoozedUntil = snoozeMillis
            )
        )

        // 取消旧闹钟，重新调度到 Snooze 时间
        AlarmScheduler.rescheduleAlarm(context, taskId, snoozeTime)

        // 取消当前通知（Snooze 期间不保留旧通知，等新闹钟触发时再发）
        NotificationHelper.cancelNotification(context, taskId)

        Log.d(TAG, "Task $taskId snoozed until $snoozeTime")

        // 更新 Widget
        val allActiveEntities = db.taskDao().getAllActiveOnce()
        val allActiveTasks = allActiveEntities.map { it.toDomain() }
        val now = LocalDate.now()
        val maxUrgency = UrgencyCalculator.maxUrgency(allActiveTasks, now)

        val widgetIntent = Intent(context, UrgencyWidget::class.java).apply {
            action = UrgencyWidget.ACTION_UPDATE
            putExtra(UrgencyWidget.EXTRA_URGENCY, maxUrgency.name)
        }
        context.sendBroadcast(widgetIntent)
    }

    /**
     * 根据 Snooze action 计算新的触发时间。
     */
    private fun calculateSnoozeTime(action: String): LocalDateTime {
        val now = LocalDateTime.now()
        return when (action) {
            ACTION_SNOOZE_1H -> now.plusHours(1)
            ACTION_SNOOZE_3H -> now.plusHours(3)
            ACTION_SNOOZE_TOMORROW -> {
                val tomorrow = LocalDate.now().plusDays(1)
                LocalDateTime.of(tomorrow, DEFAULT_NOTIFY_TIME)
            }
            else -> now.plusHours(1) // fallback
        }
    }
}