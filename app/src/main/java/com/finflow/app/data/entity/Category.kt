package com.finflow.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Category entity for organizing expenses
 * User-created categories with custom colors and emojis
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: String,
    val description: String = "",
    val userId: Long, // Changed from String to Long
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
