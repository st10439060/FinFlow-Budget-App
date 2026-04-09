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
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Budget
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GoalsFragment : Fragment() {

    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var seekbarMinGoal: SeekBar
    private lateinit var seekbarMaxGoal: SeekBar
    private lateinit var seekbarBudget: SeekBar
    private lateinit var tvMinGoalValue: TextView
    private lateinit var tvMaxGoalValue: TextView
    private lateinit var tvBudgetValue: TextView
    private lateinit var tvGoalMonth: TextView
    private lateinit var btnSaveGoal: MaterialButton

    private var selectedCategoryId: Long = 0
    private var currentUserId: Long = 1L
    private var selectedMonthYear: String = ""
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_goals_add, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Calendar.getInstance().time)
        initializeViews(view)
        setupSeekBars()
        loadCurrentUserId()
        setupCategoryDropdown()
        setupSaveButton()
    }

    private fun initializeViews(view: View) {
        etCategory = view.findViewById(R.id.et_category)
        seekbarMinGoal = view.findViewById(R.id.seekbar_min_goal)
        seekbarMaxGoal = view.findViewById(R.id.seekbar_max_goal)
        seekbarBudget = view.findViewById(R.id.seekbar_budget)
        tvMinGoalValue = view.findViewById(R.id.tv_min_goal_value)
        tvMaxGoalValue = view.findViewById(R.id.tv_max_goal_value)
        tvBudgetValue = view.findViewById(R.id.tv_budget_value)
        tvGoalMonth = view.findViewById(R.id.tv_goal_month)
        btnSaveGoal = view.findViewById(R.id.btn_save_goal)

        val displayMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        tvGoalMonth.text = "Goals for $displayMonth"
        updateGoalLabels()
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
                loadExistingGoalForCategory()
            }
        }
    }

    private fun setupSeekBars() {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateGoalLabels()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }

        seekbarMinGoal.setOnSeekBarChangeListener(listener)
        seekbarMaxGoal.setOnSeekBarChangeListener(listener)
        seekbarBudget.setOnSeekBarChangeListener(listener)
    }

    private fun updateGoalLabels() {
        tvMinGoalValue.text = currencyFormat.format(seekbarMinGoal.progress)
        tvMaxGoalValue.text = currencyFormat.format(seekbarMaxGoal.progress)
        tvBudgetValue.text = currencyFormat.format(seekbarBudget.progress)
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
        val budgetAmount = seekbarBudget.progress

        return when {
            selectedCategoryId == 0L -> {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                false
            }
            budgetAmount <= 0 -> {
                Toast.makeText(requireContext(), "Please set a target budget", Toast.LENGTH_SHORT).show()
                false
            }
            minGoal > maxGoal -> {
                Toast.makeText(requireContext(), "Minimum goal cannot exceed maximum goal", Toast.LENGTH_SHORT).show()
                false
            }
            budgetAmount < minGoal || budgetAmount > maxGoal -> {
                Toast.makeText(requireContext(), "Target budget must sit between min and max", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun loadExistingGoalForCategory() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val existingBudget = db.budgetDao().getBudgetForCategory(selectedCategoryId, selectedMonthYear)
            if (existingBudget != null) {
                seekbarMinGoal.progress = existingBudget.minGoal.toInt()
                seekbarMaxGoal.progress = existingBudget.maxGoal.toInt()
                seekbarBudget.progress = existingBudget.budgetAmount.toInt()
            } else {
                seekbarMinGoal.progress = 0
                seekbarMaxGoal.progress = 0
                seekbarBudget.progress = 0
            }
            updateGoalLabels()
        }
    }

    private fun saveBudgetGoal() {
        val budget = Budget(
            categoryId = selectedCategoryId,
            userId = currentUserId,
            monthYear = selectedMonthYear,
            budgetAmount = seekbarBudget.progress.toDouble(),
            minGoal = seekbarMinGoal.progress.toDouble(),
            maxGoal = seekbarMaxGoal.progress.toDouble()
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.budgetDao().insertBudget(budget)
            Toast.makeText(requireContext(), "Monthly goal saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }
}

