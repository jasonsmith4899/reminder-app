package com.mason.reminder.scheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mason.reminder.MainActivity
import com.mason.reminder.R
import com.mason.reminder.data.model.ReminderLevel
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.scheduler.CompleteActionReceiver.Companion.ACTION_COMPLETE
import com.mason.reminder.scheduler.CompleteActionReceiver.Companion.EXTRA_TASK_ID_COMPLETE
import com.mason.reminder.scheduler.SnoozeHandler.Companion.ACTION_SNOOZE_1H
import com.mason.reminder.scheduler.SnoozeHandler.Companion.ACTION_SNOOZE_3H
import com.mason.reminder.scheduler.SnoozeHandler.Companion.ACTION_SNOOZE_TOMORROW
import com.mason.reminder.scheduler.SnoozeHandler.Companion.EXTRA_TASK_ID_SNOOZE

/**
 * 构建/更新提醒通知。
 *
 * 核心职责：
 * - 轻度：发一条单次通知，不更新，可自动取消
 * - 中度/重度/自定义：倒计时更新通知（固定 notificationId = taskId）
 * - 到期日当天通知变为 "今天到期！"，setOngoing 防止滑掉
 *
 * 通知 Action 按钮：
 * - COMPLETE：标记完成（所有通知都有）
 * - SNOOZE_1H / SNOOZE_3H / SNOOZE_TOMORROW：稍后提醒（中度及以上或到期日才有）
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val GROUP_KEY = "reminder_group"
    private const val SUMMARY_NOTIFICATION_ID = 0 // Group summary 使用特殊 ID，不与 taskId 冲突

    /**
     * 发送或更新提醒通知。
     *
     * @param context      Context
     * @param taskId       任务 ID（同时用作 notificationId）
     * @param title        任务标题
     * @param level        提醒级别
     * @param urgency      当前紧急度
     * @param daysLeft     剩余天数（0 = 今天到期，负数 = 已过期）
     * @param isDueToday   是否到期日当天
     */
    fun showNotification(
        context: Context,
        taskId: Long,
        title: String,
        level: ReminderLevel,
        urgency: UrgencyState,
        daysLeft: Int,
        isDueToday: Boolean
    ) {
        val notificationId = taskId.toInt()
        val channelId = NotificationChannelRegistrar.channelIdForLevel(level)
        val nm = context.getSystemService(NotificationManager::class.java)

        // ── 内容文本 ──
        val contentText = when {
            daysLeft < 0  -> "已过期 ${-daysLeft} 天！"
            isDueToday    -> "今天到期！"
            else          -> "还剩 $daysLeft 天"
        }

        // ── 构建 Notification ──
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setGroup(GROUP_KEY)
            .setOnlyAlertOnce(true) // 更新时不重复响铃/振动
            .setPriority(urgencyToPriority(urgency))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setSilent(!isDueToday && level == ReminderLevel.LIGHT) // 轻度非到期日静默更新

        // 到期日或中度及以上：setOngoing，不让用户轻易滑掉
        if (isDueToday || level != ReminderLevel.LIGHT) {
            builder.setOngoing(true)
        }

        // 轻度非到期日：可自动取消
        if (!isDueToday && level == ReminderLevel.LIGHT) {
            builder.setAutoCancel(true)
        }

        // 到期日当天加重振动和声音
        if (isDueToday) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            builder.setSilent(false)
        }

        // ── 点击通知 → 打开 MainActivity ──
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("task_id", taskId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        builder.setContentIntent(
            PendingIntent.getActivity(
                context,
                notificationId,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        // ── Action 按钮：标记完成 ──
        builder.addAction(
            buildActionIntent(
                context, notificationId, taskId,
                ACTION_COMPLETE, EXTRA_TASK_ID_COMPLETE,
                "标记完成", CompleteActionReceiver::class.java
            )
        )

        // ── Action 按钮：稍后提醒（中度/重度/自定义/到期日才显示）──
        if (level != ReminderLevel.LIGHT || isDueToday) {
            builder.addAction(
                buildActionIntent(
                    context, notificationId, taskId,
                    ACTION_SNOOZE_1H, EXTRA_TASK_ID_SNOOZE,
                    "1小时后", SnoozeHandler::class.java
                )
            )
            builder.addAction(
                buildActionIntent(
                    context, notificationId, taskId,
                    ACTION_SNOOZE_3H, EXTRA_TASK_ID_SNOOZE,
                    "3小时后", SnoozeHandler::class.java
                )
            )
            builder.addAction(
                buildActionIntent(
                    context, notificationId, taskId,
                    ACTION_SNOOZE_TOMORROW, EXTRA_TASK_ID_SNOOZE,
                    "明天提醒", SnoozeHandler::class.java
                )
            )
        }

        nm.notify(notificationId, builder.build())

        // 更新 Group Summary（Android 7.0+ 分组通知折叠显示用）
        showGroupSummary(context)
    }

    /**
     * 取消指定任务的通知。
     */
    fun cancelNotification(context: Context, taskId: Long) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(taskId.toInt())
        // 取消后也刷新 Group Summary（让分组同步）
        showGroupSummary(context)
    }

    // ── 内部 ──────────────────────────────────────────

    /**
     * 发送 Group Summary Notification。
     *
     * Android 7.0+ 通知分组后，子通知会折叠到 Summary 下面。
     * Summary 本身不显示为独立通知（setGroupSummary(true)），
     * 仅作为分组的"父容器"。使用 IMPORTANCE_MIN 确保无声音/振动。
     */
    private fun showGroupSummary(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)

        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_LIGHT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("提醒助手")
            .setContentText("你有待处理的提醒事项")
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()

        nm.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    private fun urgencyToPriority(urgency: UrgencyState): Int = when (urgency) {
        UrgencyState.CALM    -> NotificationCompat.PRIORITY_DEFAULT
        UrgencyState.NOTICE  -> NotificationCompat.PRIORITY_HIGH
        UrgencyState.URGENT  -> NotificationCompat.PRIORITY_HIGH
        UrgencyState.CRITICAL -> NotificationCompat.PRIORITY_MAX
    }

    private fun buildActionIntent(
        context: Context,
        requestCodeBase: Int,
        taskId: Long,
        action: String,
        extraKey: String,
        label: String,
        receiverClass: Class<*>
    ): NotificationCompat.Action {
        val intent = Intent(context, receiverClass).apply {
            this.action = action
            putExtra(extraKey, taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeBase + action.hashCode() % 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(0, label, pendingIntent).build()
    }
}