package com.finflow.app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.finflow.app.data.local.entities.Category
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.data.repository.FinFlowRepository
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: FinFlowRepository) : ViewModel() {

    fun getAllExpenses(userId: Long): LiveData<List<Expense>> {
        return repository.getAllExpenses(userId).asLiveData()
    }

    fun getCategories(userId: Long): LiveData<List<Category>> {
        return repository.getAllCategories(userId).asLiveData()
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }
}
