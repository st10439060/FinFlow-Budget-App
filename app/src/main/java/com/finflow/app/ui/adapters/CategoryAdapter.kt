package com.finflow.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.finflow.app.R
import com.finflow.app.data.local.entities.Category

/**
 * RecyclerView adapter for displaying the user's categories in ManageCategoriesFragment.
 * Supports item deletion via a callback.
 */
class CategoryAdapter(
    private val onDeleteClick: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.CategoryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEmoji: TextView = itemView.findViewById(R.id.tv_category_emoji)
        private val tvName: TextView = itemView.findViewById(R.id.tv_category_name)
        private val tvDescription: TextView = itemView.findViewById(R.id.tv_category_description)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btn_delete_category)

        fun bind(category: Category) {
            tvEmoji.text = category.emoji
            tvName.text = category.name
            tvDescription.text = category.description.ifEmpty { "No description" }
            btnDelete.setOnClickListener { onDeleteClick(category) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(oldItem: Category, newItem: Category) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Category, newItem: Category) =
                oldItem == newItem
        }
    }
}
