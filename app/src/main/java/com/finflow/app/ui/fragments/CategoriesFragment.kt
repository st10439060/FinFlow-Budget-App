package com.finflow.app.ui.fragments

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Category
import kotlinx.coroutines.launch

/**
 * CategoriesFragment - Kobe
 * Displays all categories for the logged-in user
 * Allows adding, editing, and deleting categories
 */
class CategoriesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAddCategory: FloatingActionButton
    private var currentUserId: Long = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the logged-in user's ID from SharedPreferences (set by Shuaib's login)
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)

        recyclerView = view.findViewById(R.id.rv_categories)
        tvEmpty = view.findViewById(R.id.tv_empty_categories)
        fabAddCategory = view.findViewById(R.id.fab_add_category)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Navigate to AddCategoryFragment when FAB is clicked
        fabAddCategory.setOnClickListener {
            findNavController().navigate(R.id.addCategoryFragment)
        }

        loadCategories()
    }

    // Reload categories every time the screen comes back into view
    override fun onResume() {
        super.onResume()
        loadCategories()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            // Get all categories belonging to the current user
            val categories = db.categoryDao().getAllCategories()

            if (categories.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = CategoryAdapter(categories,
                    onEdit = { category -> editCategory(category) },
                    onDelete = { category -> confirmDelete(category) }
                )
            }
        }
    }

    private fun editCategory(category: Category) {
        // Navigate to AddCategoryFragment and pass the category ID for editing
        val bundle = Bundle()
        bundle.putLong("categoryId", category.id)
        findNavController().navigate(R.id.addCategoryFragment, bundle)
    }

    private fun confirmDelete(category: Category) {
        // Show a confirmation dialog before deleting
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This will also delete all expenses in this category.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.categoryDao().deleteCategory(category)
            // Log the action for understanding of code
            android.util.Log.d("CategoriesFragment", "Deleted category: ${category.name}, id: ${category.id}")
            Toast.makeText(requireContext(), "${category.name} deleted", Toast.LENGTH_SHORT).show()
            loadCategories()
        }
    }
}

/**
 * RecyclerView Adapter for the categories list
 * Shows each category with edit and delete buttons
 */
class CategoryAdapter(
    private val categories: List<Category>,
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmoji: TextView = itemView.findViewById(R.id.tv_category_emoji)
        val tvName: TextView = itemView.findViewById(R.id.tv_category_name)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_category_description)
        val btnEdit: View = itemView.findViewById(R.id.btn_edit_category)
        val btnDelete: View = itemView.findViewById(R.id.btn_delete_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.tvEmoji.text = category.emoji
        holder.tvName.text = category.name
        holder.tvDescription.text = if (category.description.isNotEmpty()) category.description else "No description"
        holder.btnEdit.setOnClickListener { onEdit(category) }
        holder.btnDelete.setOnClickListener { onDelete(category) }
    }

    override fun getItemCount() = categories.size
}
