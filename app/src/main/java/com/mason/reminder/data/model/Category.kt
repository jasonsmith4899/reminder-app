package com.mason.reminder.data.model

import com.mason.reminder.data.db.entity.CategoryEntity

data class Category(
    val id: Long,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)