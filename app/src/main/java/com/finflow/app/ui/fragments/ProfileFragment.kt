package com.finflow.app.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.finflow.app.LoginActivity
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var tvCategoryCount: TextView
    private lateinit var tvTotalSpent: TextView
    private lateinit var btnLogout: MaterialButton

    private var currentUserId: Long = 1L
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
        loadUserProfile()
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            val user = db.userDao().getUserById(currentUserId)
            val expenseCount = db.expenseDao().getAllExpenses(currentUserId)
            val categoryCount = db.categoryDao().getCategoryCount(currentUserId)
            val totalSpent = db.expenseDao().getTotalSpentInRange(
                currentUserId,
                0,
                System.currentTimeMillis()
            ) ?: 0.0

            Toast.makeText(
                requireContext(),
                "Profile loaded for ${user?.username}. Expenses: $expenseCount, Categories: $categoryCount, Total: ${currencyFormat.format(totalSpent)}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun logout() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        sharedPref.edit().remove("current_user_id").apply()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
