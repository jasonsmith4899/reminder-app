package com.mason.reminder.di

import android.content.Context
import androidx.room.Room
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.db.dao.CategoryDao
import com.mason.reminder.data.db.dao.ReminderLogDao
import com.mason.reminder.data.db.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideReminderLogDao(db: AppDatabase): ReminderLogDao = db.reminderLogDao()
}