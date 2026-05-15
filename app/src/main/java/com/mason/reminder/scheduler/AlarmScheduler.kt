package com.mason.reminder.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mason.reminder.scheduler.ReminderReceiver.Companion.ACTION_REMINDER
import com.mason.reminder.scheduler.ReminderReceiver.Companion.EXTRA_TASK_ID
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 设置和取消 AlarmManager 精确闹钟。
 *
 * Android 14+ (API 34) 要求 SCHEDULE_EXACT_ALARMS 权限，默认不授予。
 * USE_EXACT_ALARM 权限（闹钟类应用可申请）则默认授予。
 *
 * 降级策略：
 * - canScheduleExactAlarms() == true → setExactAndAllowWhileIdle（精确+唤醒）
 * - canScheduleExactAlarms() == false → setAndAllowWhileIdle（不精确+唤醒，误差几分钟）
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE_OFFSET = 1000 // 避免 PendingIntent 冲突

    /**
     * 为指定 [taskId] 在 [triggerAt] 时间设置闹钟。
     *
     * 自动判断精确/不精确闹钟权限，降级时使用不精确闹钟。
     */
    fun setExactAlarm(context: Context, taskId: Long, triggerAt: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildPendingIntent(context, taskId)
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (canScheduleExactAlarms(context)) {
            // 精确闹钟：唤醒 + 精确触发
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
            Log.d(TAG, "Set EXACT alarm for task $taskId at $triggerAt")
        } else {
            // 降级：不精确闹钟，误差可能几分钟
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
            Log.d(TAG, "Set INEXACT alarm for task $taskId at $triggerAt (exact permission denied)")
        }
    }

    /**
     * 强制设置不精确闹钟（用于每日兜底等非关键场景）。
     */
    fun setInexactAlarm(context: Context, taskId: Long, triggerAt: LocalDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildPendingIntent(context, taskId)
        val triggerMillis = triggerAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
        Log.d(TAG, "Set INEXACT alarm for task $taskId at $triggerAt")
    }

    /**
     * 取消指定 [taskId] 的闹钟。
     */
    fun cancelAlarm(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = buildPendingIntent(context, taskId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled alarm for task $taskId")
    }

    /**
     * 取消指定 [taskId] 的闹钟，并在 [triggerAt] 重新设置。
     * 用于 Snooze 场景：先取消旧闹钟，再设置新时间。
     */
    fun rescheduleAlarm(context: Context, taskId: Long, triggerAt: LocalDateTime) {
        cancelAlarm(context, taskId)
        setExactAlarm(context, taskId, triggerAt)
    }

    /**
     * 检查当前应用是否有精确闹钟权限。
     *
     * Android 12+ (API 31) 引入 canScheduleExactAlarms()。
     * Android 14+ (API 34) 默认不授予 SCHEDULE_EXACT_ALARMS。
     * 如果应用持有 USE_EXCT_ALARM 权限（闹钟类应用专用），也会返回 true。
     *
     * 低于 API 31 的设备始终返回 true（无需此权限）。
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true // Android 11 及以下无需精确闹钟权限
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    /**
     * 构建打开系统精确闹钟设置页面的 Intent。
     *
     * Android 12+ (API 31) 提供 ACTION_APPLICATION_DETAILS_SETTINGS 页面，
     * 用户可在此页面手动授予 SCHEDULE_EXACT_ALARMS 权限。
     * UI 层可调用此方法获取 Intent 并通过 startActivity 打开设置页面。
     *
     * 低于 API 31 的设备不需要此权限，返回 null。
     */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        // Android 12+: 打开应用详情页（内有"闹钟和提醒"开关）
        return Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // ── 内部 ──────────────────────────────────────────

    private fun buildPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_OFFSET + taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}