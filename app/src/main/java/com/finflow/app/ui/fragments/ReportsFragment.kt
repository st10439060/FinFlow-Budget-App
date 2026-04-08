package com.finflow.app.ui.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.ui.adapters.ExpenseAdapter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for viewing expenses by date range
 * Displays total spent per category and detailed expense list
 * Implements date filtering for user-selectable periods
 */
class ReportsFragment : Fragment() {

    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnLoadReports: MaterialButton
    private lateinit var tvTotalExpenses: TextView
    private lateinit var rvCategoryTotals: RecyclerView
    private lateinit var rvExpenses: RecyclerView

    private var startDateMillis: Long = 0
    private var endDateMillis: Long = System.currentTimeMillis()
    private var currentUserId: Long = 1L

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupDatePickers()
        setupRecyclerViews()
        loadCurrentUserId()

        btnLoadReports.setOnClickListener {
            loadExpenseReports()
        }

        // Set default date range (last 30 days)
        val calendar = Calendar.getInstance()
        endDateMillis = calendar.timeInMillis
        etEndDate.setText(dateFormat.format(calendar.time))

        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDateMillis = calendar.timeInMillis
        etStartDate.setText(dateFormat.format(calendar.time))

        // Auto-load on start
        loadExpenseReports()
    }

    private fun initializeViews(view: View) {
        etStartDate = view.findViewById(R.id.et_start_date)
        etEndDate = view.findViewById(R.id.et_end_date)
        btnLoadReports = view.findViewById(R.id.btn_load_reports)
        tvTotalExpenses = view.findViewById(R.id.tv_total_expenses)
        rvCategoryTotals = view.findViewById(R.id.rv_category_totals)
        rvExpenses = view.findViewById(R.id.rv_expenses)
    }

    private fun setupDatePickers() {
        etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDateMillis

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    startDateMillis = calendar.timeInMillis
                    etStartDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = endDateMillis

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    endDateMillis = calendar.timeInMillis
                    etEndDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupRecyclerViews() {
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvCategoryTotals.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadExpenseReports() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            // Load expenses in date range
            val expenses = db.expenseDao().getExpensesByDateRange(
                currentUserId,
                startDateMillis,
                endDateMillis
            )

            // Calculate total
            val total = expenses.sumOf { it.amount }
            tvTotalExpenses.text = "Total: ${currencyFormat.format(total)}"

            // Group by category
            val categoryTotals = expenses.groupBy { it.categoryId }
                .map { (categoryId, expenses) ->
                    val category = db.categoryDao().getCategoryById(categoryId)
                    val categoryTotal = expenses.sumOf { it.amount }
                    "${category?.emoji} ${category?.name}: ${currencyFormat.format(categoryTotal)}"
                }

            // Display category totals
            rvCategoryTotals.adapter = CategoryTotalAdapter(categoryTotals)

            // Display all expenses
            rvExpenses.adapter = ExpenseAdapter(expenses) { expense ->
                // Handle expense click - show photo if available
            }
        }
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    // Simple adapter for category totals
    private class CategoryTotalAdapter(private val items: List<String>) :
        RecyclerView.Adapter<CategoryTotalAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = items[position]
            holder.textView.textSize = 16f
        }

        override fun getItemCount() = items.size
    }
}
