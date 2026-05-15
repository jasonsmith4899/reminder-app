package com.mason.reminder.data.repository

import com.mason.reminder.data.db.dao.TaskDao
import com.mason.reminder.data.model.Task
import com.mason.reminder.data.model.UrgencyState
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    fun getById(id: Long): Flow<Task?> =
        taskDao.getById(id).map { it?.toDomain() }

    suspend fun getByIdOnce(id: Long): Task? =
        taskDao.getByIdOnce(id)?.toDomain()

    fun getByCategory(categoryId: Long): Flow<List<Task>> =
        taskDao.getByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    fun getAllActive(): Flow<List<Task>> =
        taskDao.getAllActive().map { list -> list.map { it.toDomain() } }

    fun getAllInactive(): Flow<List<Task>> =
        taskDao.getAllInactive().map { list -> list.map { it.toDomain() } }

    fun getDueInRange(from: Long, to: Long): Flow<List<Task>> =
        taskDao.getDueInRange(from, to).map { list -> list.map { it.toDomain() } }

    suspend fun insert(task: Task): Long =
        taskDao.insert(task.toEntity())

    suspend fun update(task: Task) =
        taskDao.update(task.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(task: Task) =
        taskDao.delete(task.toEntity())

    suspend fun markCompleted(id: Long) =
        taskDao.markCompleted(id, completedAt = System.currentTimeMillis())

    /** Calculate urgency for a single task */
    fun calculateUrgency(task: Task, now: LocalDate): UrgencyState {
        val dueLocalDate = java.time.Instant.ofEpochMilli(task.nextDueDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val daysLeft = dueLocalDate.toEpochDay() - now.toEpochDay()
        return when {
            daysLeft <= 0  -> UrgencyState.CRITICAL
            daysLeft <= 1  -> UrgencyState.URGENT
            daysLeft <= 3  -> UrgencyState.NOTICE
            else           -> UrgencyState.CALM
        }
    }

    /** Get the global max urgency across all active tasks */
    fun calculateGlobalUrgency(tasks: List<Task>, now: LocalDate): UrgencyState {
        if (tasks.isEmpty()) return UrgencyState.CALM
        return tasks.map { calculateUrgency(it, now) }.maxByOrNull { it.ordinal }!!
    }

    /** Tasks due within the next N days */
    fun getUpcomingTasks(days: Int): Flow<List<Task>> {
        val now = LocalDate.now()
        val from = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val to = now.plusDays(days.toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return getDueInRange(from, to)
    }
}