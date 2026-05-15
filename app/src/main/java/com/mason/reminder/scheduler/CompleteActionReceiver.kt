package com.mason.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.ReminderType
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.ReminderCalculator
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.toLocalDate
import com.mason.reminder.widget.UrgencyWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 处理通知上的"标记完成"按钮点击。
 *
 * 核心职责：
 * 1. 将任务标记为已完成（isActive = false）或计算下次到期日（循环任务）
 * 2. 取消当前闹钟和通知
 * 3. 如果是循环任务，计算新的 nextDueDate 并重新调度闹钟
 * 4. 记录 reminder_log（action = COMPLETED）
 * 5. 更新 Widget 颜色（全局紧急度可能变化）
 */
class CompleteActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.mason.reminder.ACTION_COMPLETE"
        const val EXTRA_TASK_ID_COMPLETE = "com.mason.reminder.EXTRA_TASK_ID_COMPLETE"
        private val TAG = "CompleteActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID_COMPLETE, -1L)
        if (taskId == -1L) {
            Log.w(TAG, "Invalid taskId in complete action")
            return
        }

        Log.d(TAG, "Marking task $taskId as completed")

        // goAsync() 给予 ~10s 保护窗口，防止 onReceive 返回后进程被杀
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleComplete(context, taskId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleComplete(context: Context, taskId: Long) {
        val db = AppDatabase.getInstance(context)
        val taskEntity = db.taskDao().getByIdOnce(taskId)

        if (taskEntity == null) {
            Log.w(TAG, "Task $taskId not found")
            AlarmScheduler.cancelAlarm(context, taskId)
            NotificationHelper.cancelNotification(context, taskId)
            return
        }

        val task = taskEntity.toDomain()

        // 记录 reminder_log：COMPLETED
        db.reminderLogDao().insert(
            com.mason.reminder.data.db.entity.ReminderLogEntity(
                taskId = task.id,
                notifiedAt = System.currentTimeMillis(),
                daysBeforeDue = 0,
                actionTaken = "COMPLETED"
            )
        )

        // 取消当前闹钟和通知
        AlarmScheduler.cancelAlarm(context, taskId)
        NotificationHelper.cancelNotification(context, taskId)

        if (task.reminderType == ReminderType.RECURRING) {
            // 循环任务：计算新的 nextDueDate，保持活跃
            val newNextDueDate = ReminderCalculator.nextDueDate(task)
            db.taskDao().update(
                taskEntity.copy(
                    nextDueDate = newNextDueDate,
                    lastCompletedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Recurring task $taskId completed, new nextDueDate: ${newNextDueDate.toLocalDate()}")

            // 为新的到期日调度闹钟
            val newTask = taskEntity.copy(nextDueDate = newNextDueDate).toDomain()
            val firstNotifyTime = com.mason.reminder.util.ReminderCalculator.firstNotifyTime(
                dueDate = newNextDueDate.toLocalDate(),
                level = newTask.reminderLevel,
                customAdvanceDays = newTask.customAdvanceDays
            )
            AlarmScheduler.setExactAlarm(context, taskId, firstNotifyTime)
        } else {
            // 单次任务：标记为已完成（isActive = false）
            db.taskDao().markCompleted(taskId, completedAt = System.currentTimeMillis())
            Log.d(TAG, "One-time task $taskId marked as completed")
        }

        // 更新 Widget 颜色（全局紧急度可能变化）
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
}