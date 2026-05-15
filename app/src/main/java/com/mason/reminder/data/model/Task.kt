package com.mason.reminder.data.model

import com.mason.reminder.data.db.entity.TaskEntity

enum class ReminderType { ONCE, RECURRING }
enum class ReminderLevel { LIGHT, MEDIUM, HEAVY, CUSTOM }
enum class IntervalUnit { DAY, WEEK, MONTH }
enum class CustomNotifyFreq { DAILY, EVERY_2_DAYS, WEEKLY }

data class Task(
    val id: Long,
    val title: String,
    val description: String,
    val categoryId: Long,
    val reminderType: ReminderType,
    val reminderLevel: ReminderLevel,
    val customAdvanceDays: Int?,
    val customNotifyFreq: CustomNotifyFreq?,
    val intervalValue: Int?,
    val intervalUnit: IntervalUnit?,
    val startDate: Long?,
    val dueDate: Long?,
    val nextDueDate: Long,
    val lastCompletedAt: Long?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    reminderType = ReminderType.valueOf(reminderType),
    reminderLevel = ReminderLevel.valueOf(reminderLevel),
    customAdvanceDays = customAdvanceDays,
    customNotifyFreq = customNotifyFreq?.let { CustomNotifyFreq.valueOf(it) },
    intervalValue = intervalValue,
    intervalUnit = intervalUnit?.let { IntervalUnit.valueOf(it) },
    startDate = startDate,
    dueDate = dueDate,
    nextDueDate = nextDueDate,
    lastCompletedAt = lastCompletedAt,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    reminderType = reminderType.name,
    reminderLevel = reminderLevel.name,
    customAdvanceDays = customAdvanceDays,
    customNotifyFreq = customNotifyFreq?.name,
    intervalValue = intervalValue,
    intervalUnit = intervalUnit?.name,
    startDate = startDate,
    dueDate = dueDate,
    nextDueDate = nextDueDate,
    lastCompletedAt = lastCompletedAt,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)