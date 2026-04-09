package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Category
import kotlinx.coroutines.launch

/**
 * AddCategoryFragment - Kobe
 * Used for both CREATING a new category and EDITING an existing one
 * If a categoryId is passed in the arguments, it loads that category for editing
 */
class AddCategoryFragment : Fragment() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmoji: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    private var currentUserId: Long = 1L
    private var editingCategoryId: Long = -1L // -1 means we are creating new
    private var existingCategory: Category? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get logged-in user ID from SharedPreferences (set by Shuaib's login)
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)

        etName = view.findViewById(R.id.et_category_name)
        etEmoji = view.findViewById(R.id.et_category_emoji)
        etDescription = view.findViewById(R.id.et_category_description)
        btnSave = view.findViewById(R.id.btn_save_category)
        btnCancel = view.findViewById(R.id.btn_cancel_category)

        // Check if we received a category ID for editing
        editingCategoryId = arguments?.getLong("categoryId", -1L) ?: -1L

        if (editingCategoryId != -1L) {
            // We are in EDIT mode - load the existing category
            loadCategoryForEditing(editingCategoryId)
            btnSave.text = "Update Category"
        } else {
            // We are in CREATE mode
            btnSave.text = "Save Category"
        }

        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveCategory()
            }
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadCategoryForEditing(categoryId: Long) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val category = db.categoryDao().getCategoryById(categoryId)
            category?.let {
                existingCategory = it
                // Pre-fill the fields with existing data
                etName.setText(it.name)
                etEmoji.setText(it.emoji)
                etDescription.setText(it.description)
                android.util.Log.d("AddCategoryFragment", "Loaded category for editing: ${it.name}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = etName.text.toString().trim()
        val emoji = etEmoji.text.toString().trim()

        return when {
            name.isEmpty() -> {
                etName.error = "Category name is required"
                false
            }
            emoji.isEmpty() -> {
                etEmoji.error = "Please add an emoji for this category"
                false
            }
            name.length > 30 -> {
                etName.error = "Name must be 30 characters or less"
                false
            }
            else -> true
        }
    }

    private fun saveCategory() {
        val name = etName.text.toString().trim()
        val emoji = etEmoji.text.toString().trim()
        val description = etDescription.text.toString().trim()

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            if (editingCategoryId != -1L && existingCategory != null) {
                // UPDATE existing category
                val updatedCategory = existingCategory!!.copy(
                    name = name,
                    emoji = emoji,
                    description = description
                )
                db.categoryDao().updateCategory(updatedCategory)
                android.util.Log.d("AddCategoryFragment", "Updated category: $name")
                Toast.makeText(requireContext(), "Category updated!", Toast.LENGTH_SHORT).show()
            } else {
                // INSERT new category
                val newCategory = Category(
                    name = name,
                    emoji = emoji,
                    description = description,
                    color = "#4CAF50", // default green color
                    userId = currentUserId
                )
                db.categoryDao().insertCategory(newCategory)
                android.util.Log.d("AddCategoryFragment", "Created new category: $name for user: $currentUserId")
                Toast.makeText(requireContext(), "Category created!", Toast.LENGTH_SHORT).show()
            }

            // Go back to the categories list
            findNavController().navigateUp()
        }
    }
}
