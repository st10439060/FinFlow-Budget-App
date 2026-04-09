package com.finflow.app.ui.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
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
import coil.load
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.ui.adapters.ExpenseAdapter
import com.finflow.app.utils.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReportsFragment : Fragment() {

    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnLoadReports: MaterialButton
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvExpenseCount: TextView
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
    ): View = inflater.inflate(R.layout.fragment_reports, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupDatePickers()
        setupRecyclerViews()
        loadCurrentUserId()
        setDefaultDateRange()

        btnLoadReports.setOnClickListener {
            loadExpenseReports()
        }

        loadExpenseReports()
    }

    private fun initializeViews(view: View) {
        etStartDate = view.findViewById(R.id.et_start_date)
        etEndDate = view.findViewById(R.id.et_end_date)
        btnLoadReports = view.findViewById(R.id.btn_load_reports)
        tvTotalExpenses = view.findViewById(R.id.tv_total_expenses)
        tvExpenseCount = view.findViewById(R.id.tv_expense_count)
        rvCategoryTotals = view.findViewById(R.id.rv_category_totals)
        rvExpenses = view.findViewById(R.id.rv_expenses)
    }

    private fun setDefaultDateRange() {
        val calendar = Calendar.getInstance()
        endDateMillis = DateUtils.getEndOfDay(calendar.timeInMillis)
        etEndDate.setText(dateFormat.format(calendar.time))

        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDateMillis = DateUtils.getStartOfDay(calendar.timeInMillis)
        etStartDate.setText(dateFormat.format(calendar.time))
    }

    private fun setupDatePickers() {
        etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = startDateMillis }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    startDateMillis = DateUtils.getStartOfDay(calendar.timeInMillis)
                    etStartDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etEndDate.setOnClickListener {
            val calendar = Calendar.getInstance().apply { timeInMillis = endDateMillis }
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    endDateMillis = DateUtils.getEndOfDay(calendar.timeInMillis)
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
        if (startDateMillis > endDateMillis) {
            Toast.makeText(requireContext(), "Start date must be before end date", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val expenses = db.expenseDao().getExpensesByDateRange(currentUserId, startDateMillis, endDateMillis)
            val categories = db.categoryDao().getAllCategories().associateBy { it.id }

            val total = expenses.sumOf { it.amount }
            tvTotalExpenses.text = "Total: ${currencyFormat.format(total)}"
            tvExpenseCount.text = "${expenses.size} expense(s) in selected date range"

            val categoryTotals = expenses
                .groupBy { it.categoryId }
                .map { (categoryId, groupedExpenses) ->
                    val category = categories[categoryId]
                    CategoryTotalItem(
                        emoji = category?.emoji ?: "💰",
                        name = category?.name ?: "Uncategorised",
                        total = groupedExpenses.sumOf { it.amount }
                    )
                }
                .sortedByDescending { it.total }

            rvCategoryTotals.adapter = CategoryTotalAdapter(categoryTotals)
            rvExpenses.adapter = ExpenseAdapter(
                expenses = expenses,
                categoryLabels = categories.mapValues { it.value.name },
                categoryEmojis = categories.mapValues { it.value.emoji },
                onExpenseClick = { expense -> showExpenseDetail(expense, categories[expense.categoryId]?.name) }
            )
        }
    }

    private fun showExpenseDetail(expense: Expense, categoryName: String?) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_expense_receipt)

        val tvTitle = dialog.findViewById<TextView>(R.id.tv_dialog_title)
        val tvAmount = dialog.findViewById<TextView>(R.id.tv_dialog_amount)
        val tvCategory = dialog.findViewById<TextView>(R.id.tv_dialog_category)
        val tvDate = dialog.findViewById<TextView>(R.id.tv_dialog_date)
        val tvNotes = dialog.findViewById<TextView>(R.id.tv_dialog_notes)
        val ivReceipt = dialog.findViewById<ImageView>(R.id.iv_receipt)
        val tvNoReceipt = dialog.findViewById<TextView>(R.id.tv_no_receipt)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btn_close_receipt)

        tvTitle.text = expense.description
        tvAmount.text = currencyFormat.format(expense.amount)
        tvCategory.text = categoryName ?: "Uncategorised"
        tvDate.text = DateUtils.formatDate(expense.date, "dd MMM yyyy")
        tvNotes.text = if (expense.notes.isBlank()) "No notes added" else expense.notes

        val photoPath = expense.photoPath
        if (photoPath.isNullOrBlank()) {
            ivReceipt.visibility = View.GONE
            tvNoReceipt.visibility = View.VISIBLE
        } else {
            ivReceipt.visibility = View.VISIBLE
            tvNoReceipt.visibility = View.GONE
            val model: Any = when {
                photoPath.startsWith("content://") -> Uri.parse(photoPath)
                else -> File(photoPath)
            }
            ivReceipt.load(model)
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    data class CategoryTotalItem(
        val emoji: String,
        val name: String,
        val total: Double
    )

    private inner class CategoryTotalAdapter(
        private val items: List<CategoryTotalItem>
    ) : RecyclerView.Adapter<CategoryTotalAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji: TextView = view.findViewById(R.id.tv_category_total_emoji)
            val tvName: TextView = view.findViewById(R.id.tv_category_total_name)
            val tvTotal: TextView = view.findViewById(R.id.tv_category_total_amount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_total, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvEmoji.text = item.emoji
            holder.tvName.text = item.name
            holder.tvTotal.text = currencyFormat.format(item.total)
        }

        override fun getItemCount(): Int = items.size
    }
}
