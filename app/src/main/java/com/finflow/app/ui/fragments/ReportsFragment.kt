package com.finflow.app.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
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
 *  - Bar chart of amount spent per category with min/max goal limit lines
 *  - Total amount spent in the period
 *  - Per-category spending breakdown (RecyclerView)
 *  - Full expense list; tapping an entry with a photo opens the photo
 *
 * Satisfies Part 3 requirements:
 *  - "view a graph showing the amount spent per category over a user-selectable period"
 *  - "graph must also display the minimum and maximum goals"
 */
class ReportsFragment : Fragment() {

    private val TAG = "ReportsFragment"

    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnLoadReports: MaterialButton
    private lateinit var tvTotalExpenses: TextView
    private lateinit var barChart: BarChart
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
        setupBarChart()

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
        barChart = view.findViewById(R.id.bar_chart)
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

    /** Configures the BarChart appearance before data is loaded. */
    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setPinchZoom(false)
            setScaleEnabled(true)
            animateY(600)
            legend.isEnabled = true

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = Color.parseColor("#212121")
                textSize = 10f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.parseColor("#212121")
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
        }
    }

    /**
     * Loads expenses from RoomDB for the selected date range, builds the bar chart with
     * per-category spending bars and min/max goal limit lines, and shows the expense list.
     */
    private fun loadExpenseReports() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                val expenses = db.expenseDao().getExpensesByDateRange(
                    currentUserId,
                    startDateMillis,
                    endDateMillis
                )

                Log.d(TAG, "Loaded ${expenses.size} expenses for range $startDateMillis - $endDateMillis")

                val total = expenses.sumOf { it.amount }
                tvTotalExpenses.text = "Total: ${currencyFormat.format(total)}"

                // Group spending by category
                val categoryGroups = expenses.groupBy { it.categoryId }

                // Resolve category names and load their budget goals
                val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val categoryLabels = mutableListOf<String>()
                val barEntries = mutableListOf<BarEntry>()
                val categoryTotalLines = mutableListOf<String>()

                var index = 0f
                for ((categoryId, grouped) in categoryGroups) {
                    val category = db.categoryDao().getCategoryById(categoryId)
                    val spent = grouped.sumOf { it.amount }
                    val label = if (category != null) "${category.emoji} ${category.name}" else "Unknown"

                    categoryLabels.add(label)
                    barEntries.add(BarEntry(index, spent.toFloat()))
                    categoryTotalLines.add("$label: ${currencyFormat.format(spent)}")
                    index++
                }

                // Update chart with new data
                updateBarChart(barEntries, categoryLabels, db, monthYear)

                rvCategoryTotals.adapter = CategoryTotalAdapter(categoryTotalLines)

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
     * Populates the BarChart with category spending data.
     * Adds a green LimitLine for the average minimum goal and a red LimitLine for the
     * average maximum goal so the user can visually see how spending compares to goals.
     */
    private suspend fun updateBarChart(
        entries: List<BarEntry>,
        labels: List<String>,
        db: AppDatabase,
        monthYear: String
    ) {
        if (entries.isEmpty()) {
            barChart.clear()
            barChart.invalidate()
            return
        }

        val dataSet = BarDataSet(entries, "Spending per Category").apply {
            colors = listOf(
                Color.parseColor("#2E7D32"),
                Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"),
                Color.parseColor("#F44336"),
                Color.parseColor("#9C27B0"),
                Color.parseColor("#00BCD4"),
                Color.parseColor("#795548")
            )
            valueTextColor = Color.parseColor("#212121")
            valueTextSize = 10f
        }

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = labels.size

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        barChart.data = barData

        // Load budgets for this month and compute average min/max goals to show as limit lines
        val budgets = db.budgetDao().getBudgetsForMonthList(currentUserId, monthYear)
        barChart.axisLeft.removeAllLimitLines()

        if (budgets.isNotEmpty()) {
            val avgMin = budgets.mapNotNull { it.minGoal.takeIf { v -> v > 0 } }.average()
            val avgMax = budgets.mapNotNull { it.maxGoal.takeIf { v -> v > 0 } }.average()

            if (avgMin > 0) {
                val minLine = LimitLine(avgMin.toFloat(), "Min Goal").apply {
                    lineWidth = 2f
                    lineColor = Color.parseColor("#4CAF50")
                    textColor = Color.parseColor("#4CAF50")
                    textSize = 10f
                    enableDashedLine(10f, 5f, 0f)
                }
                barChart.axisLeft.addLimitLine(minLine)
            }

            if (avgMax > 0) {
                val maxLine = LimitLine(avgMax.toFloat(), "Max Goal").apply {
                    lineWidth = 2f
                    lineColor = Color.parseColor("#F44336")
                    textColor = Color.parseColor("#F44336")
                    textSize = 10f
                    enableDashedLine(10f, 5f, 0f)
                }
                barChart.axisLeft.addLimitLine(maxLine)
            }

            Log.d(TAG, "Chart limit lines: avgMin=$avgMin, avgMax=$avgMax")
        }

        barChart.invalidate()
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
