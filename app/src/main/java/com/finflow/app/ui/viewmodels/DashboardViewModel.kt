package com.finflow.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.finflow.app.data.local.entities.Budget
import com.finflow.app.data.local.entities.Category
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.data.repository.FinFlowRepository
import com.finflow.app.utils.DateUtils
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: FinFlowRepository) : ViewModel() {

    private val _userId = MutableLiveData<Long>()
    val userId: LiveData<Long> = _userId

    fun setUserId(userId: Long) {
        _userId.value = userId
    }

    fun getCategories(userId: Long): LiveData<List<Category>> {
        return repository.getAllCategories(userId).asLiveData()
    }

    fun getBudgets(userId: Long, monthYear: String): LiveData<List<Budget>> {
        return repository.getBudgetsForMonth(userId, monthYear).asLiveData()
    }

    suspend fun getExpensesForMonth(userId: Long): List<Expense> {
        val startDate = DateUtils.getStartOfMonth()
        val endDate = DateUtils.getEndOfMonth()
        return repository.getExpensesByDateRange(userId, startDate, endDate)
    }

    suspend fun getCategorySpent(userId: Long, categoryId: Long): Double {
        val startDate = DateUtils.getStartOfMonth()
        val endDate = DateUtils.getEndOfMonth()
        return repository.getCategorySpentInRange(userId, categoryId, startDate, endDate)
    }

    suspend fun getTotalSpentThisMonth(userId: Long): Double {
        val startDate = DateUtils.getStartOfMonth()
        val endDate = DateUtils.getEndOfMonth()
        return repository.getTotalSpentInRange(userId, startDate, endDate)
    }
}
