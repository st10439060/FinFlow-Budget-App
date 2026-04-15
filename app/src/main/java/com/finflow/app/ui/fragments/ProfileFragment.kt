package com.finflow.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.finflow.app.LoginActivity
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

/**
 * ProfileFragment - displays the current user's account information and spending statistics.
 * Provides navigation to Manage Categories and a Logout button that clears the session
 * and returns to LoginActivity.
 */
class ProfileFragment : Fragment() {

    private val TAG = "ProfileFragment"

    private lateinit var tvUsername: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var tvCategoryCount: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var btnManageCategories: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var currentUserId: Long = 1L
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use the dedicated profile layout (not fragment_dashboard)
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentUserId()
        Log.d(TAG, "ProfileFragment loaded for userId=$currentUserId")

        initializeViews(view)
        setupButtons()
        loadUserProfile()
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    private fun initializeViews(view: View) {
        tvUsername = view.findViewById(R.id.tv_username)
        tvExpenseCount = view.findViewById(R.id.tv_expense_count)
        tvCategoryCount = view.findViewById(R.id.tv_category_count)
        tvTotalSpent = view.findViewById(R.id.tv_total_spent)
        btnManageCategories = view.findViewById(R.id.btn_manage_categories)
        btnLogout = view.findViewById(R.id.btn_logout)
    }

    private fun setupButtons() {
        // Navigate to the Manage Categories screen using Navigation Component
        btnManageCategories.setOnClickListener {
            Log.d(TAG, "Navigating to ManageCategoriesFragment")
            findNavController().navigate(R.id.manageCategoriesFragment)
        }

        // Logout: clear session and return to login via an Intent
        btnLogout.setOnClickListener {
            logout()
        }
    }

    /**
     * Queries RoomDB for the current user's stats and populates the UI.
     */
    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                // Fetch user record
                val user = db.userDao().getUserById(currentUserId)
                tvUsername.text = user?.username ?: "User"

                // Count of all expense records
                val expenses = db.expenseDao().getAllExpenses(currentUserId).first()
                tvExpenseCount.text = expenses.size.toString()

                // Number of categories the user has
                val categoryCount = db.categoryDao().getCategoryCount(currentUserId)
                tvCategoryCount.text = categoryCount.toString()

                // Sum of all expenses from the beginning of time
                val totalSpent = db.expenseDao().getTotalSpentInRange(
                    currentUserId,
                    startDate = 0L,
                    endDate = System.currentTimeMillis()
                ) ?: 0.0
                tvTotalSpent.text = currencyFormat.format(totalSpent)

                Log.d(TAG, "Profile loaded: user=${user?.username}, expenses=${expenses.size}, total=$totalSpent")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}")
            }
        }
    }

    /**
     * Clears the saved user session and redirects to LoginActivity.
     * Uses FLAG_ACTIVITY_CLEAR_TASK so the back stack is fully cleared.
     */
    private fun logout() {
        Log.d(TAG, "User logging out, clearing session")
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        sharedPref.edit().remove("current_user_id").apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
