package com.finflow.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Expense entity with start/end times and photo support
 * Includes all required fields for Part 2 assignment
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("userId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val categoryId: Long,
    val date: Long, // Date of expense
    val startTime: String, // Start time (HH:mm format)
    val endTime: String, // End time (HH:mm format)
    val userId: Long, // Changed from String to Long for local DB
    val notes: String = "",
    val photoPath: String? = null, // Local file path for photo
    val paymentMethod: String = "Cash",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
