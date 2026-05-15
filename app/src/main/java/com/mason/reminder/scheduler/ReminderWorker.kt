package com.mason.reminder.scheduler

import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.util.daysUntil
import com.mason.reminder.util.toLocalDate
import com.mason.reminder.widget.UrgencyWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * WorkManager 兜底 Worker。
 *
 * AlarmManager 是精确触发的主力，但小米/澎湃OS 省电策略可能杀闹钟。
 * WorkManager 更抗省电策略，作为 fallback：
 * - 每天早上检查所有活跃任务
 * - 对今天到期或已过期的任务补发通知
 * - 对本应已触发但可能被省电策略杀掉的闹钟重新调度
 * - 单任务模式：指定 taskId 做针对性检查
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: android.content.Context,
    @Assisted params: WorkerParameters,
    private val database: AppDatabase
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_DAILY_CHECK = "daily_check"
        const val WORK_NAME_DAILY = "reminder_daily_check"
        const val WORK_NAME_PREFIX_SINGLE = "reminder_single_"
        private val TAG = "ReminderWorker"
        private val DEFAULT_NOTIFY_TIME = LocalTime.of(9, 0)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val isDailyCheck = inputData.getBoolean(KEY_DAILY_CHECK, false)
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)

        Log.d(TAG, "doWork: isDailyCheck=$isDailyCheck, taskId=$taskId")

        try {
            if (isDailyCheck) {
                performDailyCheck()
            } else if (taskId != -1L) {
                performSingleTaskCheck(taskId)
            } else {
                Log.w(TAG, "No valid input data for ReminderWorker")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork failed: ${e.message}", e)
            Result.failure()
        }
    }

    /**
     * 每日兜底检查：扫描所有活跃任务，对需要通知的补发。
     */
    private suspend fun performDailyCheck() {
        val allActiveEntities = database.taskDao().getAllActiveOnce()
        val allActiveTasks = allActiveEntities.map { it.toDomain() }
        val now = LocalDate.now()

        for (task in allActiveTasks) {
            val dueDate = task.nextDueDate.toLocalDate()
            val daysLeft = dueDate.daysUntil(now).toInt()
            val urgency = UrgencyCalculator.calculate(task, now)

            if (shouldNotifyNow(task.reminderLevel, task.customAdvanceDays, daysLeft)) {
                Log.d(TAG, "Daily check: notifying task ${task.id}, daysLeft=$daysLeft")

                NotificationHelper.showNotification(
                    context = applicationContext,
                    taskId = task.id,
                    title = task.title,
                    level = task.reminderLevel,
                    urgency = urgency,
                    daysLeft = daysLeft,
                    isDueToday = daysLeft <= 0
                )

                // 如果还有后续提醒日，调度下一个闹钟
                if (daysLeft > 0 && task.reminderLevel != ReminderLevel.LIGHT) {
                    val nextNotifyTime = LocalDateTime.of(now.plusDays(1), DEFAULT_NOTIFY_TIME)
                    AlarmScheduler.setExactAlarm(applicationContext, task.id, nextNotifyTime)
                }
            }
        }

        val maxUrgency = UrgencyCalculator.maxUrgency(allActiveTasks, now)
        updateWidget(maxUrgency)
    }

    /**
     * 单任务兜底检查：确保指定任务的通知和闹钟到位。
     */
    private suspend fun performSingleTaskCheck(taskId: Long) {
        val taskEntity = database.taskDao().getByIdOnce(taskId)
        if (taskEntity == null || !taskEntity.isActive) {
            Log.d(TAG, "Single check: task $taskId not found or inactive")
            return
        }

        val task = taskEntity.toDomain()
        val now = LocalDate.now()
        val dueDate = task.nextDueDate.toLocalDate()
        val daysLeft = dueDate.daysUntil(now).toInt()
        val urgency = UrgencyCalculator.calculate(task, now)

        if (shouldNotifyNow(task.reminderLevel, task.customAdvanceDays, daysLeft)) {
            NotificationHelper.showNotification(
                context = applicationContext,
                taskId = task.id,
                title = task.title,
                level = task.reminderLevel,
                urgency = urgency,
                daysLeft = daysLeft,
                isDueToday = daysLeft <= 0
            )
        }

        // 重新调度后续闹钟
        if (daysLeft > 0 && task.reminderLevel != ReminderLevel.LIGHT) {
            val nextNotifyTime = LocalDateTime.of(now.plusDays(1), DEFAULT_NOTIFY_TIME)
            AlarmScheduler.setExactAlarm(applicationContext, task.id, nextNotifyTime)
        }
    }

    /**
     * 判断当前是否应该为该任务发通知。
     *
     * 轻度：只在到期日当天
     * 中度：到期日前 3 天起
     * 重度：到期日前 15 天起
     * 自定义：按 customAdvanceDays
     */
    private fun shouldNotifyNow(
        reminderLevel: ReminderLevel,
        customAdvanceDays: Int?,
        daysLeft: Int
    ): Boolean = when (reminderLevel) {
        ReminderLevel.LIGHT   -> daysLeft <= 0
        ReminderLevel.MEDIUM  -> daysLeft <= 3
        ReminderLevel.HEAVY   -> daysLeft <= 15
        ReminderLevel.CUSTOM  -> daysLeft <= (customAdvanceDays ?: 3)
    }

    private fun updateWidget(urgency: UrgencyState) {
        val intent = Intent(applicationContext, UrgencyWidget::class.java).apply {
            action = UrgencyWidget.ACTION_UPDATE
            putExtra(UrgencyWidget.EXTRA_URGENCY, urgency.name)
        }
        applicationContext.sendBroadcast(intent)
    }
}