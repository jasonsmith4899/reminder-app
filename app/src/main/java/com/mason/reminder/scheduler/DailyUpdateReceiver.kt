package com.mason.reminder.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.util.UrgencyCalculator
import com.mason.reminder.widget.IconUpdater
import com.mason.reminder.widget.UrgencyWidget
import com.mason.reminder.widget.WidgetColorMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 每日更新 Receiver。
 *
 * 每天定时触发（由 AlarmManager 或 WorkManager 调度），
 * 计算全局紧急度并更新 Widget 的背景颜色和任务列表。
 *
 * 也可由 AlarmManager 的 ACTION_REMINDER 间接触发（通过 UrgencyWidget.ACTION_UPDATE 广播）。
 */
class DailyUpdateReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_UPDATE = "com.mason.reminder.ACTION_DAILY_UPDATE"
        private val TAG = "DailyUpdateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")

        // 只处理每日更新 action（由 AlarmManager 通过 IconUpdater 触发）
        if (intent.action != ACTION_DAILY_UPDATE) return

        // goAsync() 给予 ~10s 保护窗口，防止 onReceive 返回后进程被杀
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetColors(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 计算全局紧急度，更新 Widget 背景颜色。
     */
    private suspend fun updateWidgetColors(context: Context) {
        val db = AppDatabase.getInstance(context)
        val allActiveEntities = db.taskDao().getAllActiveOnce()
        val allActiveTasks = allActiveEntities.map { it.toDomain() }
        val now = LocalDate.now()

        val maxUrgency = UrgencyCalculator.maxUrgency(allActiveTasks, now)
        val colorHex = WidgetColorMapper.hexFor(maxUrgency)

        Log.d(TAG, "Global urgency: $maxUrgency, color: $colorHex")

        // 发送广播给 UrgencyWidget 更新颜色
        val updateIntent = Intent(context, UrgencyWidget::class.java).apply {
            action = UrgencyWidget.ACTION_UPDATE
            putExtra(UrgencyWidget.EXTRA_URGENCY, maxUrgency.name)
        }
        context.sendBroadcast(updateIntent)

        // 调度下一次每日更新
        IconUpdater.scheduleDailyUpdate(context)
    }
}