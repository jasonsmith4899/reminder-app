package com.mason.reminder.data.model

import com.mason.reminder.data.db.entity.ReminderLogEntity

enum class ActionTaken { NOTIFIED, DISMISSED, COMPLETED, SNOOZED }

data class ReminderLog(
    val id: Long,
    val taskId: Long,
    val notifiedAt: Long,
    val daysBeforeDue: Int,
    val actionTaken: ActionTaken,
    val snoozedUntil: Long?
)

fun ReminderLogEntity.toDomain(): ReminderLog = ReminderLog(
    id = id,
    taskId = taskId,
    notifiedAt = notifiedAt,
    daysBeforeDue = daysBeforeDue,
    actionTaken = ActionTaken.valueOf(actionTaken),
    snoozedUntil = snoozedUntil
)

fun ReminderLog.toEntity(): ReminderLogEntity = ReminderLogEntity(
    id = id,
    taskId = taskId,
    notifiedAt = notifiedAt,
    daysBeforeDue = daysBeforeDue,
    actionTaken = actionTaken.name,
    snoozedUntil = snoozedUntil
)