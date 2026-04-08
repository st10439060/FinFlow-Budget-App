package com.finflow.app.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finflow.app.R
import com.finflow.app.data.local.entities.Category

class CategoryProgressAdapter :
    ListAdapter<Category, CategoryProgressAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_progress, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tv_category_emoji)
        private val tvName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvSpent: TextView = itemView.findViewById(R.id.tv_category_spent)
        private val tvPercentage: TextView = itemView.findViewById(R.id.tv_category_percentage)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_category)

        fun bind(category: Category) {
            tvEmoji.text = category.emoji
            tvName.text = category.name

            val spent = 0.0
            val budget = 1000.0
            val percentage = if (budget > 0) ((spent / budget) * 100).toInt() else 0

            tvSpent.text = "R %.2f / R %.2f".format(spent, budget)
            tvPercentage.text = "$percentage%"
            progressBar.progress = percentage

            val progressColor = when {
                percentage >= 100 -> Color.parseColor("#F44336")
                percentage >= 90 -> Color.parseColor("#FF9800")
                else -> Color.parseColor("#4CAF50")
            }
            progressBar.progressTintList = android.content.res.ColorStateList.valueOf(progressColor)
            tvPercentage.setTextColor(progressColor)
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }
    }
}
