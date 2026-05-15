package com.mason.reminder.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("category_id"),
        Index("next_due_date"),
        Index("is_active")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "reminder_type")
    val reminderType: String, // "ONCE" or "RECURRING"

    @ColumnInfo(name = "reminder_level")
    val reminderLevel: String, // "LIGHT", "MEDIUM", "HEAVY", "CUSTOM"

    @ColumnInfo(name = "custom_advance_days")
    val customAdvanceDays: Int? = null,

    @ColumnInfo(name = "custom_notify_freq")
    val customNotifyFreq: String? = null, // "DAILY", "EVERY_2_DAYS", "WEEKLY"

    @ColumnInfo(name = "interval_value")
    val intervalValue: Int? = null,

    @ColumnInfo(name = "interval_unit")
    val intervalUnit: String? = null, // "DAY", "WEEK", "MONTH"

    @ColumnInfo(name = "start_date")
    val startDate: Long? = null,

    @ColumnInfo(name = "due_date")
    val dueDate: Long? = null,

    @ColumnInfo(name = "next_due_date")
    val nextDueDate: Long,

    @ColumnInfo(name = "last_completed_at")
    val lastCompletedAt: Long? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)