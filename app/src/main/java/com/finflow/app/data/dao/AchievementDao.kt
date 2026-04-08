package com.finflow.app.data.local.dao

import androidx.room.*
import com.finflow.app.data.local.entities.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun getAllAchievements(userId: Long): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE userId = :userId AND achievementType = :type")
    suspend fun getAchievementByType(userId: Long, type: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement): Long

    @Query("SELECT SUM(pointsAwarded) FROM achievements WHERE userId = :userId")
    suspend fun getTotalPoints(userId: Long): Int?
}
