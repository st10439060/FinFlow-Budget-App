package com.finflow.app.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
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
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * DashboardFragment - shows monthly budget summary, per-category spending progress,
 * and goal compliance. Sends notifications when spending hits 90% or 100% of a
 * category's maximum goal.
 */
class DashboardFragment : Fragment() {

    private val TAG = "DashboardFragment"
    private val CHANNEL_ID = "budget_alerts"
    private val PREFS_NOTIF = "finflow_notif_prefs"

    private lateinit var viewModel: DashboardViewModel
    private lateinit var categoryAdapter: CategoryProgressAdapter

    private var userId: Long = 1L
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentUserId()
        Log.d(TAG, "Dashboard loaded for userId=$userId")

        createNotificationChannel()

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
        loadGoalCompliance(view)
    }

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

        viewModel.getCategories(userId).observe(viewLifecycleOwner) { categories ->
            Log.d(TAG, "Categories loaded: ${categories.size}")
            categoryAdapter.submitList(categories)
        }

        CoroutineScope(Dispatchers.Main).launch {
            val totalSpent = viewModel.getTotalSpentThisMonth(userId)
            val totalBudget = viewModel.getTotalMonthlyBudget(userId)

            Log.d(TAG, "Monthly spent=$totalSpent, budget=$totalBudget")

            tvBudgetAmount.text = DateUtils.formatCurrency(totalBudget)
            tvSpent.text = DateUtils.formatCurrency(totalSpent)
            tvRemaining.text = DateUtils.formatCurrency(totalBudget - totalSpent)

            val progress = if (totalBudget > 0) {
                ((totalSpent / totalBudget) * 100).toInt().coerceAtMost(100)
            } else 0
            progressBudget.progress = progress
        }
    }

    /**
     * Builds the goal compliance section and fires budget alert notifications when
     * a category's spending reaches 90% or 100% of its max goal.
     */
    private fun loadGoalCompliance(view: View) {
        val complianceContainer = view.findViewById<LinearLayout>(R.id.compliance_container)
            ?: return

        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val startOfMonth = DateUtils.getStartOfMonth()
        val endOfMonth = DateUtils.getEndOfMonth()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val budgets = withContext(Dispatchers.IO) {
                    db.budgetDao().getBudgetsForMonthList(userId, monthYear)
                }

                Log.d(TAG, "Goal compliance: found ${budgets.size} budgets for month $monthYear")

                complianceContainer.removeAllViews()

                if (budgets.isEmpty()) {
                    val noGoalsText = TextView(requireContext()).apply {
                        text = "No goals set for this month. Go to Goals tab to set min/max goals."
                        textSize = 14f
                        setTextColor(Color.parseColor("#757575"))
                        setPadding(0, 8, 0, 8)
                    }
                    complianceContainer.addView(noGoalsText)
                    return@launch
                }

                for (budget in budgets) {
                    if (budget.minGoal <= 0 && budget.maxGoal <= 0) continue

                    val category = withContext(Dispatchers.IO) {
                        db.categoryDao().getCategoryById(budget.categoryId)
                    }
                    val spent = withContext(Dispatchers.IO) {
                        db.expenseDao().getCategorySpentInRange(
                            userId, budget.categoryId, startOfMonth, endOfMonth
                        ) ?: 0.0
                    }

                    val categoryName = if (category != null) "${category.emoji} ${category.name}" else "Category"

                    // Check and send budget alert notifications
                    if (budget.maxGoal > 0) {
                        checkBudgetAlert(categoryName, budget.categoryId, spent, budget.maxGoal, monthYear)
                    }

                    val (statusText, statusColor, bgColor) = when {
                        budget.maxGoal > 0 && spent > budget.maxGoal ->
                            Triple("OVER BUDGET", "#F44336", "#FFEBEE")
                        budget.minGoal > 0 && spent < budget.minGoal ->
                            Triple("UNDER MIN", "#FF9800", "#FFF3E0")
                        else ->
                            Triple("ON TRACK", "#2E7D32", "#E8F5E9")
                    }

                    val cardView = MaterialCardView(requireContext()).apply {
                        radius = 8f
                        cardElevation = 2f
                        setCardBackgroundColor(Color.parseColor(bgColor))
                        val lp = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        lp.setMargins(0, 0, 0, 12)
                        layoutParams = lp
                    }

                    val rowLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(24, 20, 24, 20)
                    }

                    val leftLayout = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val tvCatName = TextView(requireContext()).apply {
                        text = categoryName
                        textSize = 15f
                        setTextColor(Color.parseColor("#212121"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }

                    val tvSpentRange = TextView(requireContext()).apply {
                        text = "Spent: ${currencyFormat.format(spent)}  |  Min: ${currencyFormat.format(budget.minGoal)}  Max: ${currencyFormat.format(budget.maxGoal)}"
                        textSize = 12f
                        setTextColor(Color.parseColor("#757575"))
                    }

                    leftLayout.addView(tvCatName)
                    leftLayout.addView(tvSpentRange)

                    val tvStatus = TextView(requireContext()).apply {
                        text = statusText
                        textSize = 12f
                        setTextColor(Color.parseColor(statusColor))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    rowLayout.addView(leftLayout)
                    rowLayout.addView(tvStatus)
                    cardView.addView(rowLayout)
                    complianceContainer.addView(cardView)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading goal compliance: ${e.message}")
            }
        }
    }

    /**
     * Fires a notification if spending is at 90% or 100%+ of the max goal.
     * Uses a prefs key to avoid repeating the same alert in the same month.
     */
    private fun checkBudgetAlert(categoryName: String, categoryId: Long, spent: Double, maxGoal: Double, monthYear: String) {
        val pct = (spent / maxGoal) * 100
        val prefs = requireContext().getSharedPreferences(PREFS_NOTIF, Context.MODE_PRIVATE)

        if (pct >= 100.0) {
            val key = "over_${categoryId}_$monthYear"
            if (!prefs.getBoolean(key, false)) {
                sendNotification(
                    id = (categoryId * 10 + 2).toInt(),
                    title = "Over Budget: $categoryName",
                    message = "You've exceeded your max goal of ${currencyFormat.format(maxGoal)}!"
                )
                prefs.edit().putBoolean(key, true).apply()
                Log.d(TAG, "Notification: OVER BUDGET for $categoryName")
            }
        } else if (pct >= 90.0) {
            val key = "warn_${categoryId}_$monthYear"
            if (!prefs.getBoolean(key, false)) {
                sendNotification(
                    id = (categoryId * 10 + 1).toInt(),
                    title = "Budget Warning: $categoryName",
                    message = "You've used ${pct.toInt()}% of your max goal (${currencyFormat.format(maxGoal)})."
                )
                prefs.edit().putBoolean(key, true).apply()
                Log.d(TAG, "Notification: 90% WARNING for $categoryName")
            }
        }
    }

    private fun sendNotification(id: Int, title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return
        }

        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(requireContext()).notify(id, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when spending approaches or exceeds budget goals"
            }
            val notifManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notifManager.createNotificationChannel(channel)
        }
    }
}
