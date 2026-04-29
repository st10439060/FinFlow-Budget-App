package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
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
 * GoalsFragment - lets the user set a minimum monthly spend goal, a maximum monthly spend goal,
 * and a target budget amount, all per category.
 *
 * Uses SeekBar for intuitive value selection and NumberFormat for currency display.
 * Satisfies: "The user must be able to set a minimum monthly goal for money spent, as well as a maximum goal."
 *
 * All data is saved to RoomDB (Budget entity) for local persistence.
 */
class GoalsFragment : Fragment() {

    private val TAG = "GoalsFragment"

    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var seekbarMinGoal: SeekBar
    private lateinit var seekbarMaxGoal: SeekBar
    private lateinit var seekbarBudget: SeekBar
    private lateinit var tvMinGoalValue: TextView
    private lateinit var tvMaxGoalValue: TextView
    private lateinit var tvBudgetValue: TextView
    private lateinit var btnSaveGoal: MaterialButton

    private var selectedCategoryId: Long = 0

    // User ID loaded from SharedPreferences saved at login time
    private var currentUserId: Long = 1L

    // NumberFormat for displaying currency amounts in ZAR (South African Rand)
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

        loadCurrentUserId()
        Log.d(TAG, "GoalsFragment loaded for userId=$currentUserId")

        initializeViews(view)
        setupCategoryDropdown()
        setupSeekBars()
        setupSaveButton()
    }

    /** Reads the authenticated user's ID from SharedPreferences. */
    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
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

    /**
     * Loads all categories for this user from RoomDB and populates the dropdown.
     * The selected category's ID is stored for use when saving the budget.
     */
    private fun setupCategoryDropdown() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val categories = db.categoryDao().getAllCategories()

            Log.d(TAG, "Loaded ${categories.size} categories for dropdown")

            val categoryNames = categories.map { "${it.emoji} ${it.name}" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            etCategory.setAdapter(adapter)

            etCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
                Log.d(TAG, "Category selected: ${categories[position].name} (id=$selectedCategoryId)")
            }
        }
    }

    /**
     * Attaches SeekBar listeners that update the displayed currency value in real time.
     * Each SeekBar's max is 10 000 (R10 000) giving a reasonable monthly range.
     */
    private fun setupSeekBars() {
        seekbarMinGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMinGoalValue.text = currencyFormat.format(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekbarMaxGoal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvMaxGoalValue.text = currencyFormat.format(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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

    /**
     * Validates that:
     * 1. A category has been selected.
     * 2. The minimum goal does not exceed the maximum goal.
     */
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

    /**
     * Creates a Budget record in RoomDB for the selected category and current month.
     * The monthYear field is in "yyyy-MM" format to allow filtering by month.
     */
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
            try {
                val db = AppDatabase.getDatabase(requireContext())
                db.budgetDao().insertBudget(budget)

                Log.d(TAG, "Budget saved: category=$selectedCategoryId, min=$minGoal, max=$maxGoal, budget=$budgetAmount, month=$monthYear")
                Toast.makeText(requireContext(), "Budget goal saved successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving budget: ${e.message}")
                Toast.makeText(requireContext(), "Error saving budget goal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearForm() {
        etCategory.text?.clear()
        seekbarMinGoal.progress = 0
        seekbarMaxGoal.progress = 0
        seekbarBudget.progress = 0
        selectedCategoryId = 0
    }
}
