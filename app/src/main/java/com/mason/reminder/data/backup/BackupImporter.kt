package com.mason.reminder.data.backup

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mason.reminder.data.db.AppDatabase
import java.io.InputStream

class BackupImporter(private val database: AppDatabase) {

    private val gson = Gson()

    sealed class ImportResult {
        data class Success(val categories: Int, val tasks: Int, val logs: Int) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    suspend fun importFromJson(json: String): ImportResult {
        return try {
            val data = gson.fromJson(json, BackupData::class.java)
            if (data == null) {
                ImportResult.Error("JSON 解析失败：数据为空")
            } else {
                // Insert categories first (tasks depend on them)
                data.categories.forEach { category ->
                    database.categoryDao().insert(category)
                }
                data.tasks.forEach { task ->
                    database.taskDao().insert(task)
                }
                data.reminderLogs.forEach { log ->
                    database.reminderLogDao().insert(log)
                }
                ImportResult.Success(
                    categories = data.categories.size,
                    tasks = data.tasks.size,
                    logs = data.reminderLogs.size
                )
            }
        } catch (e: JsonSyntaxException) {
            ImportResult.Error("JSON 格式错误：${e.message}")
        } catch (e: Exception) {
            ImportResult.Error("导入失败：${e.message}")
        }
    }

    suspend fun importFromStream(inputStream: InputStream): ImportResult {
        val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return importFromJson(json)
    }
}