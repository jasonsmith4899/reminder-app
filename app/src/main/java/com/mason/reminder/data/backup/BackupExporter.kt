package com.mason.reminder.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mason.reminder.data.db.AppDatabase
import com.mason.reminder.data.db.entity.CategoryEntity
import com.mason.reminder.data.db.entity.ReminderLogEntity
import com.mason.reminder.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import java.io.OutputStream

data class BackupData(
    val categories: List<CategoryEntity>,
    val tasks: List<TaskEntity>,
    val reminderLogs: List<ReminderLogEntity>,
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis()
)

class BackupExporter(private val database: AppDatabase) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    suspend fun exportToJson(): String {
        val categories = database.categoryDao().getAllSorted().first()
        val activeTasks = database.taskDao().getAllActive().first()
        val inactiveTasks = database.taskDao().getAllInactive().first()
        val logs = database.reminderLogDao().getAll().first()

        val data = BackupData(
            categories = categories,
            tasks = activeTasks + inactiveTasks,
            reminderLogs = logs
        )
        return gson.toJson(data)
    }

    suspend fun exportToStream(outputStream: OutputStream) {
        val json = exportToJson()
        outputStream.write(json.toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }
}