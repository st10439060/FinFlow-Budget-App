package com.finflow.app.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Category
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.ui.adapters.ExpenseAdapter
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * ReportsFragment - date range expense reports with bar chart, pie chart,
 * category filter dropdown, and CSV export.
 */
class ReportsFragment : Fragment() {

    private val TAG = "ReportsFragment"

    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnLoadReports: MaterialButton
    private lateinit var btnExportCsv: MaterialButton
    private lateinit var tvTotalExpenses: TextView
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var rvCategoryTotals: RecyclerView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var spinnerCategoryFilter: Spinner
    private lateinit var etSearchExpenses: TextInputEditText

    private var startDateMillis: Long = 0
    private var endDateMillis: Long = System.currentTimeMillis()
    private var currentUserId: Long = 1L
    private var selectedCategoryId: Long = -1L  // -1 = All categories

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    private var allCategories: List<Category> = emptyList()
    private var lastLoadedExpenses: List<Expense> = emptyList()

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
        setupPieChart()
        loadCategoriesForFilter()

        btnLoadReports.setOnClickListener { loadExpenseReports() }
        btnExportCsv.setOnClickListener { exportToCsv() }

        etSearchExpenses.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterExpenseList(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val calendar = Calendar.getInstance()
        endDateMillis = calendar.timeInMillis
        etEndDate.setText(dateFormat.format(calendar.time))

        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDateMillis = calendar.timeInMillis
        etStartDate.setText(dateFormat.format(calendar.time))

        loadExpenseReports()
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    private fun initializeViews(view: View) {
        etStartDate = view.findViewById(R.id.et_start_date)
        etEndDate = view.findViewById(R.id.et_end_date)
        btnLoadReports = view.findViewById(R.id.btn_load_reports)
        btnExportCsv = view.findViewById(R.id.btn_export_csv)
        tvTotalExpenses = view.findViewById(R.id.tv_total_expenses)
        barChart = view.findViewById(R.id.bar_chart)
        pieChart = view.findViewById(R.id.pie_chart)
        rvCategoryTotals = view.findViewById(R.id.rv_category_totals)
        rvExpenses = view.findViewById(R.id.rv_expenses)
        spinnerCategoryFilter = view.findViewById(R.id.spinner_category_filter)
        etSearchExpenses = view.findViewById(R.id.et_search_expenses)
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

    private fun loadCategoriesForFilter() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.categoryDao().getAllCategories(currentUserId).collect { categories ->
                allCategories = categories
                val labels = mutableListOf("All Categories")
                labels.addAll(categories.map { "${it.emoji} ${it.name}" })
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategoryFilter.adapter = adapter

                spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                        selectedCategoryId = if (pos == 0) -1L else allCategories[pos - 1].id
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { selectedCategoryId = -1L }
                }
            }
        }
    }

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

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            setHoleColor(Color.WHITE)
            setEntryLabelTextSize(11f)
            setEntryLabelColor(Color.parseColor("#212121"))
            legend.isEnabled = true
            animateY(600)
        }
    }

    private fun loadExpenseReports() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                val allExpenses = db.expenseDao().getExpensesByDateRange(currentUserId, startDateMillis, endDateMillis)

                val expenses = if (selectedCategoryId == -1L) allExpenses
                else allExpenses.filter { it.categoryId == selectedCategoryId }

                lastLoadedExpenses = expenses
                Log.d(TAG, "Loaded ${expenses.size} expenses")

                val total = expenses.sumOf { it.amount }
                tvTotalExpenses.text = "Total: ${currencyFormat.format(total)}"

                val categoryGroups = expenses.groupBy { it.categoryId }
                val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val categoryLabels = mutableListOf<String>()
                val barEntries = mutableListOf<BarEntry>()
                val pieEntries = mutableListOf<PieEntry>()
                val categoryTotalLines = mutableListOf<String>()

                var index = 0f
                for ((categoryId, grouped) in categoryGroups) {
                    val category = db.categoryDao().getCategoryById(categoryId)
                    val spent = grouped.sumOf { it.amount }
                    val label = if (category != null) "${category.emoji} ${category.name}" else "Unknown"

                    categoryLabels.add(label)
                    barEntries.add(BarEntry(index, spent.toFloat()))
                    pieEntries.add(PieEntry(spent.toFloat(), label))
                    categoryTotalLines.add("$label: ${currencyFormat.format(spent)}")
                    index++
                }

                updateBarChart(barEntries, categoryLabels, db, monthYear)
                updatePieChart(pieEntries)

                rvCategoryTotals.adapter = CategoryTotalAdapter(categoryTotalLines)
                rvExpenses.adapter = ExpenseAdapter(expenses, { expense -> handleExpenseClick(expense) }, { expense -> showEditExpenseDialog(expense) })

            } catch (e: Exception) {
                Log.e(TAG, "Error loading reports: ${e.message}")
                Toast.makeText(requireContext(), "Error loading reports", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun updateBarChart(
        entries: List<BarEntry>,
        labels: List<String>,
        db: AppDatabase,
        monthYear: String
    ) {
        if (entries.isEmpty()) { barChart.clear(); barChart.invalidate(); return }

        val dataSet = BarDataSet(entries, "Spending per Category").apply {
            colors = listOf(
                Color.parseColor("#2E7D32"), Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"), Color.parseColor("#F44336"),
                Color.parseColor("#00897B"), Color.parseColor("#00BCD4"),
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

        val budgets = db.budgetDao().getBudgetsForMonthList(currentUserId, monthYear)
        barChart.axisLeft.removeAllLimitLines()

        if (budgets.isNotEmpty()) {
            val avgMin = budgets.mapNotNull { it.minGoal.takeIf { v -> v > 0 } }.average()
            val avgMax = budgets.mapNotNull { it.maxGoal.takeIf { v -> v > 0 } }.average()

            if (avgMin > 0) {
                barChart.axisLeft.addLimitLine(LimitLine(avgMin.toFloat(), "Min Goal").apply {
                    lineWidth = 2f; lineColor = Color.parseColor("#4CAF50")
                    textColor = Color.parseColor("#4CAF50"); textSize = 10f
                    enableDashedLine(10f, 5f, 0f)
                })
            }
            if (avgMax > 0) {
                barChart.axisLeft.addLimitLine(LimitLine(avgMax.toFloat(), "Max Goal").apply {
                    lineWidth = 2f; lineColor = Color.parseColor("#F44336")
                    textColor = Color.parseColor("#F44336"); textSize = 10f
                    enableDashedLine(10f, 5f, 0f)
                })
            }
        }

        barChart.invalidate()
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        if (entries.isEmpty()) { pieChart.clear(); pieChart.invalidate(); return }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#2E7D32"), Color.parseColor("#2196F3"),
                Color.parseColor("#FF9800"), Color.parseColor("#F44336"),
                Color.parseColor("#00897B"), Color.parseColor("#00BCD4"),
                Color.parseColor("#795548")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 11f
        }

        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    /**
     * Exports the currently loaded expenses to a CSV file in the Downloads folder.
     */
    private fun exportToCsv() {
        if (lastLoadedExpenses.isEmpty()) {
            Toast.makeText(requireContext(), "No expenses to export", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fileName = "finflow_expenses_${System.currentTimeMillis()}.csv"

                val csvLines = buildString {
                    appendLine("Date,Description,Category,Amount,Notes")
                    for (expense in lastLoadedExpenses) {
                        val category = db.categoryDao().getCategoryById(expense.categoryId)
                        val catName = category?.name ?: "Unknown"
                        val date = dateTimeFmt.format(Date(expense.date))
                        val desc = expense.description.replace(",", ";")
                        val notes = (expense.notes ?: "").replace(",", ";")
                        appendLine("$date,$desc,$catName,${expense.amount},$notes")
                    }
                }

                writeCSVFile(fileName, csvLines)
                Toast.makeText(requireContext(), "Exported: $fileName", Toast.LENGTH_LONG).show()
                Log.d(TAG, "CSV exported: $fileName")

            } catch (e: Exception) {
                Log.e(TAG, "CSV export error: ${e.message}")
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun writeCSVFile(fileName: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri: Uri? = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                requireContext().contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer -> writer.write(content) }
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            File(downloadsDir, fileName).writeText(content)
        }
    }

    private fun handleExpenseClick(expense: Expense) {
        val photoPath = expense.photoPath
        if (!photoPath.isNullOrEmpty()) {
            val file = File(photoPath)
            if (file.exists()) showPhotoDialog(Uri.fromFile(file), expense.description)
            else Toast.makeText(requireContext(), "Photo file not found", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "No photo for this expense", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPhotoDialog(photoUri: Uri, title: String) {
        val imageView = ImageView(requireContext()).apply {
            setImageURI(photoUri)
            adjustViewBounds = true
            setPadding(16, 16, 16, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title).setView(imageView).setPositiveButton("Close", null).show()
    }

    private fun filterExpenseList(query: String) {
        val filtered = if (query.isBlank()) lastLoadedExpenses
        else lastLoadedExpenses.filter {
            it.description.contains(query, ignoreCase = true) ||
                    (it.notes ?: "").contains(query, ignoreCase = true)
        }
        rvExpenses.adapter = ExpenseAdapter(filtered, { expense -> handleExpenseClick(expense) }, { expense -> showEditExpenseDialog(expense) })
    }

    /**
     * Shows a dialog to edit the description, amount, and notes of an existing expense.
     * Long-press any expense row to trigger this.
     */
    private fun showEditExpenseDialog(expense: Expense) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.activity_list_item, null)

        val etDesc = android.widget.EditText(requireContext()).apply { setText(expense.description); hint = "Description" }
        val etAmt = android.widget.EditText(requireContext()).apply { setText(expense.amount.toString()); hint = "Amount"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val etNotes = android.widget.EditText(requireContext()).apply { setText(expense.notes ?: ""); hint = "Notes" }

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(etDesc)
            addView(etAmt)
            addView(etNotes)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newDesc = etDesc.text.toString().trim()
                val newAmt = etAmt.text.toString().toDoubleOrNull() ?: expense.amount
                val newNotes = etNotes.text.toString().trim()

                if (newDesc.isEmpty()) {
                    Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val db = AppDatabase.getDatabase(requireContext())
                    val updated = expense.copy(description = newDesc, amount = newAmt, notes = newNotes)
                    db.expenseDao().updateExpense(updated)
                    Toast.makeText(requireContext(), "Expense updated", Toast.LENGTH_SHORT).show()
                    loadExpenseReports()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

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
