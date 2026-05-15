package com.mason.reminder.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mason.reminder.data.db.dao.CategoryDao
import com.mason.reminder.data.db.dao.ReminderLogDao
import com.mason.reminder.data.db.dao.TaskDao
import com.mason.reminder.data.db.entity.CategoryEntity
import com.mason.reminder.data.db.entity.ReminderLogEntity
import com.mason.reminder.data.db.entity.TaskEntity

@Database(
    entities = [
        CategoryEntity::class,
        TaskEntity::class,
        ReminderLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderLogDao(): ReminderLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取 AppDatabase 单例。
         * BroadcastReceiver / Worker 无法使用 Hilt @Inject，通过此方法获取。
         * Hilt DatabaseModule 也使用此单例，确保唯一实例。
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "reminder_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}