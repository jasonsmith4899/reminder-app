package com.mason.reminder.data.repository

import com.mason.reminder.data.db.dao.ReminderLogDao
import com.mason.reminder.data.model.ActionTaken
import com.mason.reminder.data.model.ReminderLog
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderLogDao: ReminderLogDao
) {

    fun getByTask(taskId: Long): Flow<List<ReminderLog>> =
        reminderLogDao.getByTask(taskId).map { list -> list.map { it.toDomain() } }

    suspend fun getByTaskOnce(taskId: Long): List<ReminderLog> =
        reminderLogDao.getByTaskOnce(taskId).map { it.toDomain() }

    fun getAll(): Flow<List<ReminderLog>> =
        reminderLogDao.getAll().map { list -> list.map { it.toDomain() } }

    suspend fun logNotification(
        taskId: Long,
        daysBeforeDue: Int,
        actionTaken: ActionTaken,
        snoozedUntil: Long? = null
    ): Long {
        val log = ReminderLog(
            id = 0,
            taskId = taskId,
            notifiedAt = System.currentTimeMillis(),
            daysBeforeDue = daysBeforeDue,
            actionTaken = actionTaken,
            snoozedUntil = snoozedUntil
        )
        return reminderLogDao.insert(log.toEntity())
    }
}