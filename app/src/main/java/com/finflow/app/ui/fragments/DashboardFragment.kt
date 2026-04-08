package com.finflow.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.repository.FinFlowRepository
import com.finflow.app.ui.adapters.CategoryProgressAdapter
import com.finflow.app.ui.viewmodels.DashboardViewModel
import com.finflow.app.ui.viewmodels.DashboardViewModelFactory
import com.finflow.app.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var categoryAdapter: CategoryProgressAdapter
    private var userId: Long = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = FinFlowRepository(
            database.categoryDao(),
            database.expenseDao(),
            database.budgetDao(),
            database.achievementDao(),
            database.userProgressDao()
        )

        viewModel = ViewModelProvider(
            this,
            DashboardViewModelFactory(repository)
        )[DashboardViewModel::class.java]

        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeDefaultCategories(userId)
        }

        setupRecyclerView(view)
        observeData(view)
    }

    private fun setupRecyclerView(view: View) {
        val rvCategories = view.findViewById<RecyclerView>(R.id.rv_categories)
        categoryAdapter = CategoryProgressAdapter()
        rvCategories.adapter = categoryAdapter
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeData(view: View) {
        val tvBudgetAmount = view.findViewById<TextView>(R.id.tv_budget_amount)
        val tvSpent = view.findViewById<TextView>(R.id.tv_spent)
        val tvRemaining = view.findViewById<TextView>(R.id.tv_remaining)
        val progressBudget = view.findViewById<ProgressBar>(R.id.progress_budget)

        viewModel.getCategories(userId).observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val totalSpent = viewModel.getTotalSpentThisMonth(userId)
            val totalBudget = 5000.0

            tvBudgetAmount.text = DateUtils.formatCurrency(totalBudget)
            tvSpent.text = DateUtils.formatCurrency(totalSpent)
            tvRemaining.text = DateUtils.formatCurrency(totalBudget - totalSpent)
            progressBudget.progress = ((totalSpent / totalBudget) * 100).toInt()
        }
    }
}
