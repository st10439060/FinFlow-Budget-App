package com.finflow.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Budget entity with minimum and maximum monthly goals
 * Supports per-category budgets with goal tracking
 */
@Entity(
    tableName = "budgets",
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
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val userId: Long, // Changed from String to Long
    val monthYear: String, // Format: "YYYY-MM"
    val budgetAmount: Double, // Target budget amount
    val minGoal: Double = 0.0, // Minimum spending goal
    val maxGoal: Double = 0.0, // Maximum spending goal
    val rolloverEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
