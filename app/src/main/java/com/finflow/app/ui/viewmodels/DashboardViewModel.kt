package com.finflow.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.finflow.app.data.local.entities.Budget
import com.finflow.app.data.local.entities.Category
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.data.repository.FinFlowRepository
import com.finflow.app.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for the Dashboard screen.
 * Provides spending totals, budget totals, and category lists for the current user.
 */
class DashboardViewModel(private val repository: FinFlowRepository) : ViewModel() {

    private val _userId = MutableLiveData<Long>()
    val userId: LiveData<Long> = _userId

    fun setUserId(userId: Long) {
        _userId.value = userId
    }

    /** Returns a LiveData list of all categories belonging to [userId]. */
    fun getCategories(userId: Long): LiveData<List<Category>> {
        return repository.getAllCategories(userId).asLiveData()
    }

    /** Returns a LiveData list of budgets for the given month. */
    fun getBudgets(userId: Long, monthYear: String): LiveData<List<Budget>> {
        return repository.getBudgetsForMonth(userId, monthYear).asLiveData()
    }

    /** Returns all expenses for [userId] in the current calendar month. */
    suspend fun getExpensesForMonth(userId: Long): List<Expense> {
        val startDate = DateUtils.getStartOfMonth()
        val endDate = DateUtils.getEndOfMonth()
        return repository.getExpensesByDateRange(userId, startDate, endDate)
    }

    /** Returns total amount spent for a specific category this month. */
    suspend fun getCategorySpent(userId: Long, categoryId: Long): Double {
        val startDate = DateUtils.getStartOfMonth()
        val endDate = DateUtils.getEndOfMonth()
        return repository.getCategorySpentInRange(userId, categoryId, startDate, endDate)
    }

    /** Returns the sum of all expense amounts for [userId] in the current month. */
    suspend fun getTotalSpentThisMonth(userId: Long): Double {
        val startDate = DateUtils.getStartOfMonth()
        val endDate = DateUtils.getEndOfMonth()
        return repository.getTotalSpentInRange(userId, startDate, endDate)
    }

    /**
     * Returns the sum of all budget amounts set for [userId] in the current calendar month.
     * Uses "yyyy-MM" format to match the Budget entity's monthYear field.
     */
    suspend fun getTotalMonthlyBudget(userId: Long): Double {
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        return repository.getTotalMonthlyBudget(userId, monthYear)
    }
}
