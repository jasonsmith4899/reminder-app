package com.mason.reminder.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mason.reminder.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getByIdOnce(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE category_id = :categoryId AND is_active = 1 ORDER BY next_due_date ASC")
    fun getByCategory(categoryId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_active = 1 ORDER BY next_due_date ASC")
    fun getAllActive(): Flow<List<TaskEntity>>

    /** 一次性获取所有活跃任务（用于 Widget / BroadcastReceiver 等非 Flow 场景） */
    @Query("SELECT * FROM tasks WHERE is_active = 1 ORDER BY next_due_date ASC")
    suspend fun getAllActiveOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE is_active = 1 AND next_due_date <= :threshold ORDER BY next_due_date ASC")
    fun getDueBefore(threshold: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_active = 1 AND next_due_date BETWEEN :from AND :to ORDER BY next_due_date ASC")
    fun getDueInRange(from: Long, to: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_active = 0 ORDER BY updated_at DESC")
    fun getAllInactive(): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET is_active = 0, last_completed_at = :completedAt, updated_at = :updatedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Long, updatedAt: Long = System.currentTimeMillis())
}