package com.finflow.app.data.local.dao

import androidx.room.*
import com.finflow.app.data.local.entities.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun getUserProgress(userId: Long): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    suspend fun getUserProgressSync(userId: Long): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(userProgress: UserProgress)

    @Update
    suspend fun updateProgress(userProgress: UserProgress)
}
