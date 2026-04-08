package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Budget
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for setting monthly budget goals with min/max limits
 * Uses SeekBar widgets for intuitive value selection
 * Implements NumberFormat for currency display
 */
class GoalsFragment : Fragment() {

    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var seekbarMinGoal: SeekBar
    private lateinit var seekbarMaxGoal: SeekBar
    private lateinit var seekbarBudget: SeekBar
    private lateinit var tvMinGoalValue: TextView
    private lateinit var tvMaxGoalValue: TextView
    private lateinit var tvBudgetValue: TextView
    private lateinit var btnSaveGoal: MaterialButton

    private var selectedCategoryId: Long = 0
    private var currentUserId: Long = 1L
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goals_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupCategoryDropdown()
        setupSeekBars()
        setupSaveButton()
        loadCurrentUserId()
    }

    private fun initializeViews(view: View) {
        etCategory = view.findViewById(R.id.et_category)
        seekbarMinGoal = view.findViewById(R.id.seekbar_min_goal)
        seekbarMaxGoal = view.findViewById(R.id.seekbar_max_goal)
        seekbarBudget = view.findViewById(R.id.seekbar_budget)
        tvMinGoalValue = view.findViewById(R.id.tv_min_goal_value)
        tvMaxGoalValue = view.findViewById(R.id.tv_max_goal_value)
        tvBudgetValue = view.findViewById(R.id.tv_budget_value)
        btnSaveGoal = view.findViewById(R.id.btn_save_goal)
    }

    private fun setupCategoryDropdown() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val categories = db.categoryDao().getAllCategories()

            val categoryNames = categories.map { "${it.emoji} ${it.name}" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            etCategory.setAdapter(adapter)

            etCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
            }
        }
    }

    private fun setupSeekBars() {
        // Min Goal SeekBar with NumberFormat
        seekbarMinGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMinGoalValue.text = currencyFormat.format(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Max Goal SeekBar with NumberFormat
        seekbarMaxGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMaxGoalValue.text = currencyFormat.format(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Budget SeekBar with NumberFormat
        seekbarBudget.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBudgetValue.text = currencyFormat.format(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSaveButton() {
        btnSaveGoal.setOnClickListener {
            if (validateInputs()) {
                saveBudgetGoal()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val minGoal = seekbarMinGoal.progress
        val maxGoal = seekbarMaxGoal.progress

        when {
            selectedCategoryId == 0L -> {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return false
            }
            minGoal > maxGoal -> {
                Toast.makeText(requireContext(), "Minimum goal cannot exceed maximum goal", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun saveBudgetGoal() {
        val minGoal = seekbarMinGoal.progress.toDouble()
        val maxGoal = seekbarMaxGoal.progress.toDouble()
        val budgetAmount = seekbarBudget.progress.toDouble()

        val calendar = Calendar.getInstance()
        val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

        val budget = Budget(
            categoryId = selectedCategoryId,
            userId = currentUserId,
            monthYear = monthYear,
            budgetAmount = budgetAmount,
            minGoal = minGoal,
            maxGoal = maxGoal
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.budgetDao().insertBudget(budget)

            Toast.makeText(requireContext(), "Budget goal saved successfully", Toast.LENGTH_SHORT).show()
            clearForm()
        }
    }

    private fun clearForm() {
        etCategory.text?.clear()
        seekbarMinGoal.progress = 0
        seekbarMaxGoal.progress = 0
        seekbarBudget.progress = 0
        selectedCategoryId = 0
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }
}

