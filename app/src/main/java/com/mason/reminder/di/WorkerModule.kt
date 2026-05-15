package com.mason.reminder.di

import androidx.work.WorkerParameters
import com.mason.reminder.scheduler.ReminderWorker
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt Worker 模块。
 *
 * @HiltWorker 的 Worker 需要通过 Hilt 进行依赖注入，
 * 此模块确保 Worker 的绑定被正确注册。
 * 注意：实际 @HiltWorker 的注入由 hilt-work 自动处理，
 * 此模块的存在是为了保持模块结构的完整性，
 * 以及后续可能需要为 Worker 提供额外依赖。
 */
@Module
@InstallIn(SingletonComponent::class)
interface WorkerModule {

    // ReminderWorker 使用 @HiltWorker + @AssistedInject，
    // hilt-work 框架会自动为其生成绑定，无需手动 @Provides。
    // 若将来 Worker 需要额外依赖（如 Repository），只需在构造参数中添加即可。
}