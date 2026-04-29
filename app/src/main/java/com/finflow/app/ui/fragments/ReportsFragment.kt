package com.finflow.app.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.ui.adapters.ExpenseAdapter
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * ReportsFragment - shows expenses for a user-selected date range.
 * Displays:
 *  - Total amount spent in the period
 *  - Per-category spending breakdown
 *  - Full expense list; tapping an entry with a photo opens the photo
 *
 * Satisfies: "view the list of all expense entries created during a user-selectable period"
 * and "view the total amount of money spent on each category during a user-selectable period"
 */
class ReportsFragment : Fragment() {

    private val TAG = "ReportsFragment"

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

        loadCurrentUserId()
        Log.d(TAG, "ReportsFragment loaded for userId=$currentUserId")

        initializeViews(view)
        setupDatePickers()
        setupRecyclerViews()

        btnLoadReports.setOnClickListener {
            loadExpenseReports()
        }

        // Default range: last 30 days
        val calendar = Calendar.getInstance()
        endDateMillis = calendar.timeInMillis
        etEndDate.setText(dateFormat.format(calendar.time))

        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDateMillis = calendar.timeInMillis
        etStartDate.setText(dateFormat.format(calendar.time))

        // Auto-load on open
        loadExpenseReports()
    }

    /** Reads user ID saved during login. */
    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
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
            val cal = Calendar.getInstance().also { it.timeInMillis = startDateMillis }
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day)
                startDateMillis = cal.timeInMillis
                etStartDate.setText(dateFormat.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        etEndDate.setOnClickListener {
            val cal = Calendar.getInstance().also { it.timeInMillis = endDateMillis }
            DatePickerDialog(requireContext(), { _, year, month, day ->
                cal.set(year, month, day, 23, 59, 59)
                endDateMillis = cal.timeInMillis
                etEndDate.setText(dateFormat.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupRecyclerViews() {
        rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        rvCategoryTotals.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Loads expenses from RoomDB for the selected date range and builds:
     *  1. Category total summaries
     *  2. Full expense list with click-to-view-photo support
     */
    private fun loadExpenseReports() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                // Fetch expenses in the selected date range for this user
                val expenses = db.expenseDao().getExpensesByDateRange(
                    currentUserId,
                    startDateMillis,
                    endDateMillis
                )

                Log.d(TAG, "Loaded ${expenses.size} expenses for range $startDateMillis - $endDateMillis")

                // Total spending
                val total = expenses.sumOf { it.amount }
                tvTotalExpenses.text = "Total: ${currencyFormat.format(total)}"

                // Group by category and resolve category names from DB
                val categoryTotals = expenses
                    .groupBy { it.categoryId }
                    .map { (categoryId, grouped) ->
                        val category = db.categoryDao().getCategoryById(categoryId)
                        val categoryTotal = grouped.sumOf { it.amount }
                        val label = if (category != null) {
                            "${category.emoji} ${category.name}"
                        } else "Unknown"
                        "$label: ${currencyFormat.format(categoryTotal)}"
                    }

                rvCategoryTotals.adapter = CategoryTotalAdapter(categoryTotals)

                // Show the expense list; clicking an entry shows its photo if one exists
                rvExpenses.adapter = ExpenseAdapter(expenses) { expense ->
                    handleExpenseClick(expense)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reports: ${e.message}")
                Toast.makeText(requireContext(), "Error loading reports", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * When an expense is clicked, show its photo in a dialog if one was captured.
     * If no photo is attached, show a brief message.
     */
    private fun handleExpenseClick(expense: Expense) {
        val photoPath = expense.photoPath
        if (!photoPath.isNullOrEmpty()) {
            val file = File(photoPath)
            if (file.exists()) {
                showPhotoDialog(Uri.fromFile(file), expense.description)
            } else {
                Toast.makeText(requireContext(), "Photo file not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "No photo for this expense", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Displays the expense receipt photo in a full-screen AlertDialog.
     */
    private fun showPhotoDialog(photoUri: Uri, title: String) {
        val imageView = ImageView(requireContext()).apply {
            setImageURI(photoUri)
            adjustViewBounds = true
            setPadding(16, 16, 16, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    /** Simple adapter that renders a list of plain text category summary lines. */
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
