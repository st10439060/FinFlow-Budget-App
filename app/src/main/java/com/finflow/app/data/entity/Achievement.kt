package com.finflow.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "achievements",
    indices = [Index("userId")]
)
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val achievementType: String,
    val title: String,
    val description: String,
    val pointsAwarded: Int,
    val unlockedAt: Long = System.currentTimeMillis()
)
