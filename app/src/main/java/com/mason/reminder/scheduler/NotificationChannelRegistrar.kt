package com.mason.reminder.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.mason.reminder.data.model.ReminderLevel

/**
 * 注册四个 NotificationChannel，分别对应轻度/中度/重度/自定义提醒。
 *
 * 轻度 (LIGHT)：IMPORTANCE_DEFAULT，无振动，温和提醒
 * 中度 (MEDIUM)：IMPORTANCE_HIGH，振动，倒计时通知
 * 重度 (HEAVY)：IMPORTANCE_HIGH，强制振动 + 系统紧急声音，紧急到期
 * 自定义 (CUSTOM)：IMPORTANCE_HIGH，振动，自定义提前天数和通知频率
 *
 * 必须在 Application.onCreate 中调用，否则 Android 8.0+ 通知不显示。
 */
object NotificationChannelRegistrar {

    // Channel IDs — 4 个 Channel 对应 4 种 ReminderLevel
    const val CHANNEL_LIGHT = "reminder_light"
    const val CHANNEL_MEDIUM = "reminder_medium"
    const val CHANNEL_HEAVY = "reminder_heavy"
    const val CHANNEL_CUSTOM = "reminder_custom"

    // Channel 名称（用户在系统设置中可见）
    private const val CHANNEL_LIGHT_NAME = "轻度提醒"
    private const val CHANNEL_MEDIUM_NAME = "中度提醒"
    private const val CHANNEL_HEAVY_NAME = "重度提醒"
    private const val CHANNEL_CUSTOM_NAME = "自定义提醒"

    // Channel 描述
    private const val CHANNEL_LIGHT_DESC = "到期当天提醒一次，温和提示"
    private const val CHANNEL_MEDIUM_DESC = "提前3天开始倒计时提醒，带振动"
    private const val CHANNEL_HEAVY_DESC = "提前15天倒计时，到期日高优先级提醒，振动+声音"
    private const val CHANNEL_CUSTOM_DESC = "自定义提前天数和通知频率"

    /**
     * 在 Application.onCreate 中调用，注册所有 Channel。
     * Android 8.0+ 必须在发通知前注册 Channel，否则通知不显示。
     * 重复调用不会覆盖已创建的 Channel（用户可能已修改设置）。
     */
    fun registerAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(NotificationManager::class.java)

        val channels = listOf(
            buildLightChannel(),
            buildMediumChannel(),
            buildHeavyChannel(),
            buildCustomChannel()
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }

    /**
     * 根据 ReminderLevel 返回对应的 Channel ID。
     * 每种级别都有独立的 NotificationChannel。
     */
    fun channelIdForLevel(level: ReminderLevel): String = when (level) {
        ReminderLevel.LIGHT   -> CHANNEL_LIGHT
        ReminderLevel.MEDIUM  -> CHANNEL_MEDIUM
        ReminderLevel.HEAVY   -> CHANNEL_HEAVY
        ReminderLevel.CUSTOM  -> CHANNEL_CUSTOM
    }

    // ── 内部构建 ──────────────────────────────────────────

    private fun buildLightChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_LIGHT,
            CHANNEL_LIGHT_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_LIGHT_DESC
            enableVibration(false)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
    }

    private fun buildMediumChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_MEDIUM,
            CHANNEL_MEDIUM_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_MEDIUM_DESC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
    }

    private fun buildHeavyChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_HEAVY,
            CHANNEL_HEAVY_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_HEAVY_DESC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 100, 300, 100, 300)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            // 重度用默认闹钟声音，即使在静音模式下也能响铃
            val alarmAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, alarmAttrs)
        }
    }

    private fun buildCustomChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_CUSTOM,
            CHANNEL_CUSTOM_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_CUSTOM_DESC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
    }
}