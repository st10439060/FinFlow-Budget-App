package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Category
import com.finflow.app.ui.adapters.CategoryAdapter
import kotlinx.coroutines.launch

/**
 * ManageCategoriesFragment - lets users create and delete their own expense categories.
 * Satisfies the requirement: "The user must be able to create categories that expense
 * and budget entries will belong to."
 *
 * Uses RoomDB (CategoryDao) for all data persistence.
 */
class ManageCategoriesFragment : Fragment() {

    private val TAG = "ManageCategoriesFragment"

    private lateinit var etCategoryName: TextInputEditText
    private lateinit var etCategoryEmoji: TextInputEditText
    private lateinit var etCategoryDescription: TextInputEditText
    private lateinit var btnAddCategory: MaterialButton
    private lateinit var rvCategories: RecyclerView

    private lateinit var categoryAdapter: CategoryAdapter
    private var currentUserId: Long = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manage_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the authenticated user's ID
        loadCurrentUserId()
        Log.d(TAG, "ManageCategories loaded for userId=$currentUserId")

        initializeViews(view)
        setupRecyclerView()
        setupAddButton()
        loadCategories()
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    private fun initializeViews(view: View) {
        etCategoryName = view.findViewById(R.id.et_category_name)
        etCategoryEmoji = view.findViewById(R.id.et_category_emoji)
        etCategoryDescription = view.findViewById(R.id.et_category_description)
        btnAddCategory = view.findViewById(R.id.btn_add_category)
        rvCategories = view.findViewById(R.id.rv_categories)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            confirmDeleteCategory(category)
        }
        rvCategories.adapter = categoryAdapter
        rvCategories.layoutManager = LinearLayoutManager(requireContext())
    }

    /**
     * Observes the categories Flow from RoomDB and submits updates to the adapter.
     */
    private fun loadCategories() {
        val db = AppDatabase.getDatabase(requireContext())

        // Observe as LiveData inside lifecycleScope
        lifecycleScope.launch {
            db.categoryDao().getAllCategories(currentUserId).collect { categories ->
                Log.d(TAG, "Categories updated: ${categories.size} items")
                categoryAdapter.submitList(categories)
            }
        }
    }

    private fun setupAddButton() {
        btnAddCategory.setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            val emoji = etCategoryEmoji.text.toString().trim()
            val description = etCategoryDescription.text.toString().trim()

            if (!validateInputs(name, emoji)) return@setOnClickListener

            addCategory(name, emoji, description)
        }
    }

    private fun validateInputs(name: String, emoji: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    /**
     * Inserts a new Category into RoomDB for the current user.
     */
    private fun addCategory(name: String, emoji: String, description: String) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())

                val category = Category(
                    name = name,
                    emoji = emoji.ifEmpty { "📁" },
                    color = "#4CAF50",
                    description = description,
                    userId = currentUserId,
                    sortOrder = 99
                )

                val id = db.categoryDao().insertCategory(category)
                Log.d(TAG, "Category inserted with id=$id")

                Toast.makeText(requireContext(), "Category '$name' added!", Toast.LENGTH_SHORT).show()
                clearForm()
            } catch (e: Exception) {
                Log.e(TAG, "Error adding category: ${e.message}")
                Toast.makeText(requireContext(), "Error adding category", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting a category.
     */
    private fun confirmDeleteCategory(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Delete '${category.name}'? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                db.categoryDao().deleteCategory(category)
                Log.d(TAG, "Category deleted: ${category.name}")
                Toast.makeText(requireContext(), "'${category.name}' deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting category: ${e.message}")
                Toast.makeText(requireContext(), "Error deleting category", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearForm() {
        etCategoryName.text?.clear()
        etCategoryEmoji.text?.clear()
        etCategoryDescription.text?.clear()
    }
}
