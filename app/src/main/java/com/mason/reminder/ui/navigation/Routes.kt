package com.mason.reminder.ui.navigation

/**
 * 应用内所有导航路由的集中定义。
 *
 * 使用 sealed class 确保路由类型安全，
 * 避免在 NavGraph 和 Screen 中硬编码字符串。
 */
sealed class Routes(val route: String) {

    /** 首页 — 分类列表 */
    data object Home : Routes("home")

    /** 分类下的任务列表 */
    data object TaskList : Routes("tasks/{categoryId}") {
        fun createRoute(categoryId: Long): String = "tasks/$categoryId"
        const val CATEGORY_ID_ARG = "categoryId"
    }

    /** 新建 / 编辑任务详情 */
    data object TaskDetail : Routes("task_detail/{taskId}") {
        fun createRoute(taskId: Long): String = "task_detail/$taskId"
        const val TASK_ID_ARG = "taskId"
    }

    /** 新建 / 编辑分类 */
    data object CategoryEdit : Routes("category_edit/{categoryId}") {
        fun createRoute(categoryId: Long): String = "category_edit/$categoryId"
        const val CATEGORY_ID_ARG = "categoryId"
    }

    /** 设置页 */
    data object Settings : Routes("settings")

    /** 备份 / 恢复页 */
    data object Backup : Routes("backup")
}