package com.mason.reminder.di

import com.mason.reminder.data.repository.CategoryRepository
import com.mason.reminder.data.repository.ReminderRepository
import com.mason.reminder.data.repository.TaskRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // CategoryRepository, TaskRepository, ReminderRepository are @Singleton @Inject constructor
    // so Hilt can provide them automatically — no explicit @Provides needed.
}