package com.mason.reminder.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mason.reminder.ui.screen.BackupScreen
import com.mason.reminder.ui.screen.CategoryEditScreen
import com.mason.reminder.ui.screen.HomeScreen
import com.mason.reminder.ui.screen.SettingsScreen
import com.mason.reminder.ui.screen.TaskDetailScreen
import com.mason.reminder.ui.screen.TaskListScreen

/**
 * 应用导航图 — 连接所有页面。
 *
 * 首页 → 分类列表 (HomeScreen)
 * 点击分类 → 任务列表 (TaskListScreen)
 * 点击任务 → 任务详情 (TaskDetailScreen)
 * 设置 → 设置页 (SettingsScreen)
 * 备份 → 备份恢复 (BackupScreen)
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {

        // ── 首页：分类列表 ────────────────────────────
        composable(Routes.Home.route) {
            HomeScreen(
                onNavigateToTasks = { categoryId ->
                    navController.navigate(Routes.TaskList.createRoute(categoryId))
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.Settings.route)
                }
            )
        }

        // ── 分类下的任务列表 ──────────────────────────
        composable(
            route = Routes.TaskList.route,
            arguments = listOf(
                navArgument(Routes.TaskList.CATEGORY_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) {
            TaskListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { taskId ->
                    navController.navigate(Routes.TaskDetail.createRoute(taskId))
                }
            )
        }

        // ── 任务详情 ──────────────────────────────────
        composable(
            route = Routes.TaskDetail.route,
            arguments = listOf(
                navArgument(Routes.TaskDetail.TASK_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) {
            TaskDetailScreen(
                onSaved = { navController.popBackStack() }
            )
        }

        // ── 新建 / 编辑分类 ────────────────────────────
        composable(
            route = Routes.CategoryEdit.route,
            arguments = listOf(
                navArgument(Routes.CategoryEdit.CATEGORY_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) {
            CategoryEditScreen(
                onSaved = { navController.popBackStack() }
            )
        }

        // ── 设置页 ────────────────────────────────────
        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onBackupClick = { navController.navigate(Routes.Backup.route) }
            )
        }

        // ── 备份 / 恢复页 ─────────────────────────────
        composable(Routes.Backup.route) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
    }
}