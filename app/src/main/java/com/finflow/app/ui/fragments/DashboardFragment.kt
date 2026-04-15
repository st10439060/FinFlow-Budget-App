package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
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

/**
 * DashboardFragment - shows monthly budget summary and per-category spending progress.
 * Reads current user ID from SharedPreferences so data is always user-specific.
 */
class DashboardFragment : Fragment() {

    private val TAG = "DashboardFragment"

    private lateinit var viewModel: DashboardViewModel
    private lateinit var categoryAdapter: CategoryProgressAdapter

    // userId loaded from SharedPreferences set during login
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

        // Load the authenticated user's ID from SharedPreferences
        loadCurrentUserId()

        Log.d(TAG, "Dashboard loaded for userId=$userId")

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

        // Seed default categories for this user if they have none yet
        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeDefaultCategories(userId)
        }

        setupRecyclerView(view)
        observeData(view)
    }

    /**
     * Reads the saved user ID from SharedPreferences written at login time.
     */
    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        userId = sharedPref.getLong("current_user_id", 1L)
        Log.d(TAG, "Loaded userId=$userId from SharedPreferences")
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

        // Observe categories list for the current user
        viewModel.getCategories(userId).observe(viewLifecycleOwner) { categories ->
            Log.d(TAG, "Categories loaded: ${categories.size}")
            categoryAdapter.submitList(categories)
        }

        // Load spending totals from RoomDB for the current month
        CoroutineScope(Dispatchers.Main).launch {
            val totalSpent = viewModel.getTotalSpentThisMonth(userId)
            val totalBudget = viewModel.getTotalMonthlyBudget(userId)

            Log.d(TAG, "Monthly spent=$totalSpent, budget=$totalBudget")

            tvBudgetAmount.text = DateUtils.formatCurrency(totalBudget)
            tvSpent.text = DateUtils.formatCurrency(totalSpent)
            tvRemaining.text = DateUtils.formatCurrency(totalBudget - totalSpent)

            // Show progress as percentage; cap at 100 to avoid overflow
            val progress = if (totalBudget > 0) {
                ((totalSpent / totalBudget) * 100).toInt().coerceAtMost(100)
            } else 0
            progressBudget.progress = progress
        }
    }
}
