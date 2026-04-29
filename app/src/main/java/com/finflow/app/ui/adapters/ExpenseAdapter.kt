package com.finflow.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.finflow.app.R
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.utils.DateUtils

/**
 * RecyclerView adapter for displaying a list of Expense items.
 * Each row shows the expense description, date, amount, and a camera icon
 * if a receipt photo is attached to the entry.
 *
 * [onExpenseClick] is called when the user taps an item (e.g., to view its photo).
 */
class ExpenseAdapter(
    private val expenses: List<Expense>,
    private val onExpenseClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.bind(expense)
        holder.itemView.setOnClickListener { onExpenseClick(expense) }
    }

    override fun getItemCount() = expenses.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tv_expense_emoji)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_expense_description)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_expense_category)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_expense_date)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_expense_amount)

        /**
         * Binds an Expense to this ViewHolder.
         * Shows a 📷 camera emoji when the expense has a stored photo path,
         * so the user can see at a glance which entries have receipts.
         */
        fun bind(expense: Expense) {
            // Show camera emoji if a receipt photo was saved, otherwise default money bag
            tvEmoji.text = if (!expense.photoPath.isNullOrEmpty()) "📷" else "💰"
            tvDescription.text = expense.description
            // Category is shown as "Category #ID" until a join query fetches the name
            tvCategory.text = "Category #${expense.categoryId}"
            tvDate.text = DateUtils.formatDate(expense.date, "dd MMM")
            tvAmount.text = DateUtils.formatCurrency(expense.amount)
        }
    }
}
