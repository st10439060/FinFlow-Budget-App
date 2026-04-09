package com.finflow.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.finflow.app.R
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.utils.DateUtils

class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val categoryLabels: Map<Long, String> = emptyMap(),
    private val categoryEmojis: Map<Long, String> = emptyMap(),
    private val onExpenseClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(
            expense = expense,
            categoryLabel = categoryLabels[expense.categoryId] ?: "Uncategorised",
            categoryEmoji = categoryEmojis[expense.categoryId] ?: "💰"
        )
        holder.itemView.setOnClickListener { onExpenseClick(expense) }
    }

    override fun getItemCount() = expenses.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tv_expense_emoji)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_expense_description)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_expense_category)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_expense_date)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_expense_amount)
        private val tvReceiptIndicator: TextView = itemView.findViewById(R.id.tv_receipt_indicator)

        fun bind(expense: Expense, categoryLabel: String, categoryEmoji: String) {
            tvEmoji.text = categoryEmoji
            tvDescription.text = expense.description
            tvCategory.text = categoryLabel
            tvDate.text = DateUtils.formatDate(expense.date, "dd MMM yyyy")
            tvAmount.text = DateUtils.formatCurrency(expense.amount)
            tvReceiptIndicator.visibility = if (expense.photoPath.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }
}
