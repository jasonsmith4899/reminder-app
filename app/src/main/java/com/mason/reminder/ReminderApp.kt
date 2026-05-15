package com.mason.reminder

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mason.reminder.scheduler.NotificationChannelRegistrar
import com.mason.reminder.scheduler.ReminderWorker
import com.mason.reminder.widget.IconUpdater
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application 入口类。
 *
 * 核心职责：
 * 1. 注册 NotificationChannel（Android 8.0+ 必须）
 * 2. 配置 WorkManager（使用 HiltWorkerFactory）
 * 3. 调度每日兜底 WorkManager 检查
 * 4. 设置每日 Widget 更新的 AlarmManager 闹钟
 */
@HiltAndroidApp
class ReminderApp : Application(), Configuration.Provider {

    companion object {
        private val TAG = "ReminderApp"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // 1. 注册通知频道
        NotificationChannelRegistrar.registerAll(this)

        // 2. 初始化 WorkManager（HiltWorkerFactory 方式）
        // 注意：使用 Configuration.Provider 时，WorkManager 不会自动初始化，
        // 需要在 AndroidManifest 中移除默认的初始化 provider
        WorkManager.initialize(
            this,
            workManagerConfiguration
        )

        // 3. 调度每日兜底 WorkManager 检查
        scheduleDailyFallback()

        // 4. 设置每日 Widget 颜色更新的 AlarmManager 闹钟（通过 IconUpdater）
        IconUpdater.scheduleDailyUpdate(this)

        Log.d(TAG, "ReminderApp initialized")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    /**
     * 调度 WorkManager 每日兜底检查。
     *
     * 每 24 小时执行一次 ReminderWorker（daily_check 模式），
     * 作为 AlarmManager 的补充，防止省电策略杀闹钟。
     */
    private fun scheduleDailyFallback() {
        val dailyWork = PeriodicWorkRequestBuilder<ReminderWorker>(
            24, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        )
            .setInputData(
                androidx.work.Data.Builder()
                    .putBoolean(ReminderWorker.KEY_DAILY_CHECK, true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                ReminderWorker.WORK_NAME_DAILY,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWork
            )

        Log.d(TAG, "Scheduled daily fallback WorkManager check")
    }
}