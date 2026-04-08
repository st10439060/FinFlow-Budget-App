package com.finflow.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val date: Date,
    val description: String,
    val categoryId: Long,  // foreign key to Category table
    val photoPath: String? = null  // stores URI or file path of receipt photo
)