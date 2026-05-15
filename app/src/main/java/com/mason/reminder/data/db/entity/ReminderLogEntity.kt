package com.mason.reminder.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("task_id")]
)
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long,

    @ColumnInfo(name = "notified_at")
    val notifiedAt: Long,

    @ColumnInfo(name = "days_before_due")
    val daysBeforeDue: Int,

    @ColumnInfo(name = "action_taken")
    val actionTaken: String, // "DISMISSED", "COMPLETED", "SNOOZED"

    @ColumnInfo(name = "snoozed_until")
    val snoozedUntil: Long? = null
)