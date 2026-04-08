package com.finflow.app.data.local.dao

import androidx.room.*
import com.finflow.app.data.local.entities.Budget
import kotlinx.coroutines.flow.Flow

/**
 * DAO for budget operations
 * Updated to use Long userId for local authentication
 */
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    fun getBudgetsForMonth(userId: Long, monthYear: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND monthYear = :monthYear")
    suspend fun getBudgetForCategory(categoryId: Long, monthYear: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("SELECT SUM(budgetAmount) FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    suspend fun getTotalMonthlyBudget(userId: Long, monthYear: String): Double?
}
