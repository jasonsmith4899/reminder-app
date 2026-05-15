package com.mason.reminder.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.scheduler.DailyUpdateReceiver
import com.mason.reminder.util.UrgencyCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 每日更新 Widget 颜色状态的工具类。
 *
 * 职责：
 * 1. 每天早上 09:00 通过 AlarmManager 触发 DailyUpdateReceiver
 * 2. DailyUpdateReceiver 收到广播后更新所有 Widget
 * 3. 也可手动调用 refreshWidget() 立即刷新（如任务状态变更后）
 *
 * 调度策略：
 * - 有 SCHEDULE_EXACT_ALARM 权限 → 精确闹钟 09:00
 * - 否则 → 不精确闹钟（误差几分钟）
 */
object IconUpdater {

    private val DAILY_TIME = LocalTime.of(9, 0)
    private const val DAILY_REQUEST_CODE = 9999

    /**
     * 设置每日定时更新闹钟。
     * 应在 BootReceiver / Application onCreate 中调用。
     *
     * 注意：这里不能使用 AlarmScheduler（它专为任务提醒构建 PendingIntent），
     * 而是直接使用 AlarmManager，PendingIntent 目标是 DailyUpdateReceiver。
     */
    fun scheduleDailyUpdate(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildDailyPendingIntent(context)

        // 先取消旧的闹钟（防止重复）
        alarmManager.cancel(pendingIntent)

        val now = LocalDateTime.now()
        val triggerAt = if (now.toLocalTime() >= DAILY_TIME) {
            LocalDateTime.of(now.toLocalDate().plusDays(1), DAILY_TIME)
        } else {
            LocalDateTime.of(now.toLocalDate(), DAILY_TIME)
        }
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault())
            .toInstant().toEpochMilli()

        // 判断是否有精确闹钟权限
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 及以下无需此权限
        }

        if (canExact) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } else {
            // 降级：不精确闹钟
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    /**
     * 取消每日更新闹钟。
     */
    fun cancelDailyUpdate(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildDailyPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * 立即刷新所有 Widget（颜色 + 任务列表）。
     * 在任务增删改、标记完成等操作后调用。
     */
    fun refreshWidget(context: Context) {
        val intent = Intent(context, UrgencyWidget::class.java).apply {
            action = UrgencyWidget.ACTION_UPDATE
        }
        context.sendBroadcast(intent)
    }

    /**
     * 构建 DailyUpdateReceiver 的 PendingIntent（供 AlarmManager 使用）。
     */
    fun buildDailyPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyUpdateReceiver::class.java).apply {
            action = DailyUpdateReceiver.ACTION_DAILY_UPDATE
        }
        return PendingIntent.getBroadcast(
            context,
            DAILY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 计算当前全局紧急度（供外部组件如通知、Shortcut 等调用）。
     *
     * 注意：这是一个 suspend 函数，调用者需在协程中执行。
     * Widget 更新请使用 refreshWidget() 触发广播刷新，而非此方法。
     */
    suspend fun computeGlobalUrgency(context: Context): UrgencyState {
        val db = AppDatabase.getInstance(context)
        val allActiveEntities = db.taskDao().getAllActiveOnce()
        val tasks = allActiveEntities.map { it.toDomain() }
        val now = LocalDate.now()
        return UrgencyCalculator.maxUrgency(tasks, now)
    }

    /**
     * 计算全局紧急度并返回对应的颜色值。
     * 供需要直接获取颜色值的外部组件使用。
     */
    suspend fun computeGlobalColor(context: Context): Int {
        return WidgetColorMapper.colorFor(computeGlobalUrgency(context))
    }
}