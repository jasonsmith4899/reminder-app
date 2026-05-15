package com.mason.reminder.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import com.mason.reminder.data.db.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SettingsUiState(
    val defaultNotifyHour: Int = 9,
    val defaultNotifyMinute: Int = 0,
    val vibrationEnabled: Boolean = true,
    val exactAlarmEnabled: Boolean = false,
    val autoStartHintVisible: Boolean = true,
    val appVersion: String = "1.0.0"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // 检查精确闹钟权限（Android 12+）
        val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            _uiState.value = _uiState.value.copy(
                exactAlarmEnabled = alarmManager.canScheduleExactAlarms()
            )
        } else {
            _uiState.value = _uiState.value.copy(exactAlarmEnabled = true)
        }

        // 尝试读取包信息中的版本号
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            _uiState.value = _uiState.value.copy(appVersion = pkgInfo.versionName ?: "1.0.0")
        } catch (_: Exception) { /* 保持默认 */ }
    }

    fun setDefaultNotifyHour(hour: Int) {
        _uiState.value = _uiState.value.copy(defaultNotifyHour = hour)
    }

    fun setDefaultNotifyMinute(minute: Int) {
        _uiState.value = _uiState.value.copy(defaultNotifyMinute = minute)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(vibrationEnabled = enabled)
    }

    fun dismissAutoStartHint() {
        _uiState.value = _uiState.value.copy(autoStartHintVisible = false)
    }

    /** 打开系统自启动设置页 */
    fun openAutoStartSettings() {
        // 常见厂商的自启动设置 Intent
        val intents = listOf(
            // 小米
            Intent().setComponent(
                android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            // 华为
            Intent().setComponent(
                android.content.ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            ),
            // OPPO
            Intent().setComponent(
                android.content.ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            // vivo
            Intent().setComponent(
                android.content.ComponentName(
                    "com.vivo.abe",
                    "com.vivo.abe.BgStartUpManagerActivity"
                )
            ),
            // 兜底：应用详情页
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.resolveActivity(intent, 0) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) { continue }
        }
    }

    /** 打开精确闹钟设置页（Android 12+） */
    fun openExactAlarmSettings() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) { /* 无法打开 */ }
        }
    }

    /** 打开通知设置页 */
    fun openNotificationSettings() {
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // 降级到应用详情页
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}