package com.mason.reminder.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mason.reminder.data.db.entity.ReminderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ReminderLogEntity): Long

    @Query("SELECT * FROM reminder_logs WHERE task_id = :taskId ORDER BY notified_at DESC")
    fun getByTask(taskId: Long): Flow<List<ReminderLogEntity>>

    @Query("SELECT * FROM reminder_logs WHERE task_id = :taskId ORDER BY notified_at DESC")
    suspend fun getByTaskOnce(taskId: Long): List<ReminderLogEntity>

    @Query("SELECT * FROM reminder_logs ORDER BY notified_at DESC")
    fun getAll(): Flow<List<ReminderLogEntity>>
}