package com.mason.reminder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mason.reminder.ui.navigation.NavGraph
import com.mason.reminder.ui.navigation.Routes
import com.mason.reminder.ui.theme.ReminderAppTheme
import com.mason.reminder.widget.UrgencyWidget
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReminderAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val controller = rememberNavController()
                    navController = controller

                    // 处理 onCreate 时的 Widget 深链接
                    val initialRoute = resolveWidgetRoute(intent)
                    if (initialRoute != null) {
                        LaunchedEffect(initialRoute) {
                            controller.navigate(initialRoute) {
                                popUpTo(Routes.Home.route) { inclusive = false }
                            }
                        }
                    }

                    NavGraph(navController = controller)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Activity 已在栈中，新 Intent 来自 Widget 点击 → 直接导航
        val route = resolveWidgetRoute(intent)
        if (route != null && ::navController.isInitialized) {
            navController.navigate(route) {
                popUpTo(Routes.Home.route) { inclusive = false }
            }
        }
    }

    /**
     * 解析 Widget 点击 Intent → 导航 route。
     *
     * - ACTION_OPEN_TASK + EXTRA_TASK_ID → TaskDetail(taskId)
     * - ACTION_NEW_TASK                   → TaskDetail(0) 新建任务
     */
    private fun resolveWidgetRoute(intent: Intent?): String? {
        return when (intent?.action) {
            UrgencyWidget.ACTION_OPEN_TASK -> {
                val taskId = intent.getLongExtra(UrgencyWidget.EXTRA_TASK_ID, 0L)
                Routes.TaskDetail.createRoute(taskId)
            }
            UrgencyWidget.ACTION_NEW_TASK -> {
                Routes.TaskDetail.createRoute(0L)
            }
            else -> null
        }
    }
}