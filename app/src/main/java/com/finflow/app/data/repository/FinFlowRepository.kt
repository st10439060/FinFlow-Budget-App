package com.finflow.app.data.repository

import com.finflow.app.data.local.dao.*
import com.finflow.app.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class FinFlowRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val budgetDao: BudgetDao,
    private val achievementDao: AchievementDao,
    private val userProgressDao: UserProgressDao
) {

    fun getAllCategories(userId: Long): Flow<List<Category>> = categoryDao.getAllCategories(userId)
    suspend fun getCategoryById(id: Long) = categoryDao.getCategoryById(id)
    suspend fun insertCategory(category: Category) = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
    suspend fun getCategoryCount(userId: Long) = categoryDao.getCategoryCount(userId)

    fun getAllExpenses(userId: Long): Flow<List<Expense>> = expenseDao.getAllExpenses(userId)
    suspend fun getExpenseById(id: Long) = expenseDao.getExpenseById(id)
    suspend fun getExpensesByDateRange(userId: Long, startDate: Long, endDate: Long): List<Expense> =
        expenseDao.getExpensesByDateRange(userId, startDate, endDate)
    fun getExpensesByCategory(userId: Long, categoryId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(userId, categoryId)
    suspend fun getTotalSpentInRange(userId: Long, startDate: Long, endDate: Long) =
        expenseDao.getTotalSpentInRange(userId, startDate, endDate) ?: 0.0
    suspend fun getCategorySpentInRange(userId: Long, categoryId: Long, startDate: Long, endDate: Long) =
        expenseDao.getCategorySpentInRange(userId, categoryId, startDate, endDate) ?: 0.0
    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    suspend fun getExpenseCountInRange(userId: Long, startDate: Long, endDate: Long) =
        expenseDao.getExpenseCountInRange(userId, startDate, endDate)

    fun getBudgetsForMonth(userId: Long, monthYear: String): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(userId, monthYear)
    suspend fun getBudgetForCategory(categoryId: Long, monthYear: String) =
        budgetDao.getBudgetForCategory(categoryId, monthYear)
    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
    suspend fun getTotalMonthlyBudget(userId: Long, monthYear: String) =
        budgetDao.getTotalMonthlyBudget(userId, monthYear) ?: 0.0

    fun getAllAchievements(userId: Long): Flow<List<Achievement>> =
        achievementDao.getAllAchievements(userId)
    suspend fun getAchievementByType(userId: Long, type: String) =
        achievementDao.getAchievementByType(userId, type)
    suspend fun insertAchievement(achievement: Achievement) =
        achievementDao.insertAchievement(achievement)
    suspend fun getTotalPoints(userId: Long) =
        achievementDao.getTotalPoints(userId) ?: 0

    fun getUserProgress(userId: Long): Flow<UserProgress?> =
        userProgressDao.getUserProgress(userId)
    suspend fun getUserProgressSync(userId: Long) =
        userProgressDao.getUserProgressSync(userId)
    suspend fun insertOrUpdateProgress(userProgress: UserProgress) =
        userProgressDao.insertOrUpdateProgress(userProgress)

    suspend fun initializeDefaultCategories(userId: Long) {
        if (getCategoryCount(userId) == 0) {
            val defaultCategories = listOf(
                Category(name = "Groceries", emoji = "🛒", color = "#4CAF50", description = "Food and household items", userId = userId, sortOrder = 0),
                Category(name = "Transport", emoji = "🚗", color = "#2196F3", description = "Fuel, public transport, Uber", userId = userId, sortOrder = 1),
                Category(name = "Dining Out", emoji = "🍽️", color = "#FF9800", description = "Restaurants and takeaways", userId = userId, sortOrder = 2),
                Category(name = "Entertainment", emoji = "🎬", color = "#9C27B0", description = "Movies, games, hobbies", userId = userId, sortOrder = 3),
                Category(name = "Healthcare", emoji = "⚕️", color = "#F44336", description = "Medical expenses", userId = userId, sortOrder = 4),
                Category(name = "Utilities", emoji = "💡", color = "#607D8B", description = "Electricity, water, internet", userId = userId, sortOrder = 5)
            )
            defaultCategories.forEach { insertCategory(it) }
        }
    }
}
