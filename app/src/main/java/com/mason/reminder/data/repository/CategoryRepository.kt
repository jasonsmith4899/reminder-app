package com.mason.reminder.data.repository

import com.mason.reminder.data.db.dao.CategoryDao
import com.mason.reminder.data.db.entity.CategoryEntity
import com.mason.reminder.data.model.Category
import com.mason.reminder.data.model.toDomain
import com.mason.reminder.data.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllSorted(): Flow<List<Category>> =
        categoryDao.getAllSorted().map { list -> list.map { it.toDomain() } }

    fun getById(id: Long): Flow<Category?> =
        categoryDao.getById(id).map { it?.toDomain() }

    suspend fun getByIdOnce(id: Long): Category? =
        categoryDao.getByIdOnce(id)?.toDomain()

    suspend fun insert(category: Category): Long {
        val maxSort = categoryDao.getMaxSortOrder() ?: -1
        val entity = category.toEntity().copy(sortOrder = maxSort + 1)
        return categoryDao.insert(entity)
    }

    suspend fun update(category: Category) =
        categoryDao.update(category.toEntity().copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(category: Category) =
        categoryDao.delete(category.toEntity())

    suspend fun deleteById(id: Long) {
        categoryDao.getByIdOnce(id)?.let { categoryDao.delete(it) }
    }
}