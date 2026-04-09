package com.finflow.app.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.finflow.app.R
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Expense
import com.finflow.app.ui.viewmodels.ExpenseViewModel
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for adding new expenses
 * Includes date, start/end times, description, category, and photo
 * UPDATED BY KOBE: Added gallery photo picker option
 */
class AddExpenseFragment : Fragment() {

    private val expenseViewModel: ExpenseViewModel by viewModels()

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etCategory: AutoCompleteTextView
    private lateinit var etDate: TextInputEditText
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var btnAddPhoto: MaterialButton
    private lateinit var btnSave: MaterialButton

    private var selectedDate: Calendar = Calendar.getInstance()
    private var startTime: String = ""
    private var endTime: String = ""
    private var photoUri: Uri? = null
    private var photoPath: String? = null
    private var selectedCategoryId: Long = 0
    private var currentUserId: Long = 1L

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Camera permission launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera launcher
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                ivPhotoPreview.setImageURI(uri)
                ivPhotoPreview.visibility = View.VISIBLE
                photoPath = uri.path
                android.util.Log.d("AddExpenseFragment", "Photo taken, path: $photoPath")
            }
        }
    }

    // ADDED BY KOBE - Gallery launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                photoUri = uri
                ivPhotoPreview.setImageURI(uri)
                ivPhotoPreview.visibility = View.VISIBLE
                photoPath = uri.toString()
                android.util.Log.d("AddExpenseFragment", "Photo picked from gallery, uri: $photoPath")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupDatePicker()
        setupTimePickers()
        setupCategoryDropdown()
        setupPhotoCapture()
        setupSaveButton()
        loadCurrentUserId()
    }

    private fun initializeViews(view: View) {
        etAmount = view.findViewById(R.id.et_amount)
        etDescription = view.findViewById(R.id.et_description)
        etCategory = view.findViewById(R.id.et_category)
        etDate = view.findViewById(R.id.et_date)
        etStartTime = view.findViewById(R.id.et_start_time)
        etEndTime = view.findViewById(R.id.et_end_time)
        etNotes = view.findViewById(R.id.et_notes)
        ivPhotoPreview = view.findViewById(R.id.iv_photo_preview)
        btnAddPhoto = view.findViewById(R.id.btn_add_photo)
        btnSave = view.findViewById(R.id.btn_save)

        etDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate.set(year, month, day)
                    etDate.setText(dateFormat.format(selectedDate.time))
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    startTime = String.format("%02d:%02d", hour, minute)
                    etStartTime.setText(startTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        etEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    endTime = String.format("%02d:%02d", hour, minute)
                    etEndTime.setText(endTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    private fun setupCategoryDropdown() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val categories = db.categoryDao().getAllCategories()

            val categoryNames = categories.map { "${it.emoji} ${it.name}" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
            etCategory.setAdapter(adapter)

            etCategory.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
            }
        }
    }

    /**
     * UPDATED BY KOBE - Now shows a dialog to choose between Camera and Gallery
     */
    private fun setupPhotoCapture() {
        btnAddPhoto.setOnClickListener {
            // Show a dialog asking Camera or Gallery
            AlertDialog.Builder(requireContext())
                .setTitle("Add Photo")
                .setItems(arrayOf("Take Photo (Camera)", "Choose from Gallery")) { _, which ->
                    when (which) {
                        0 -> checkCameraPermissionAndOpen()
                        1 -> openGallery()
                    }
                }
                .show()
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        photoPath = photoFile.absolutePath

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        takePictureLauncher.launch(intent)
    }

    // ADDED BY KOBE - Open gallery to pick an image
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return File.createTempFile("EXPENSE_${timestamp}_", ".jpg", storageDir)
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveExpense()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val amountText = etAmount.text.toString()
        val description = etDescription.text.toString()

        when {
            amountText.isEmpty() -> {
                Toast.makeText(requireContext(), "Please enter amount", Toast.LENGTH_SHORT).show()
                return false
            }
            description.isEmpty() -> {
                Toast.makeText(requireContext(), "Please enter description", Toast.LENGTH_SHORT).show()
                return false
            }
            selectedCategoryId == 0L -> {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return false
            }
            startTime.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select start time", Toast.LENGTH_SHORT).show()
                return false
            }
            endTime.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select end time", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun saveExpense() {
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val description = etDescription.text.toString()
        val notes = etNotes.text.toString()

        val expense = Expense(
            amount = amount,
            description = description,
            categoryId = selectedCategoryId,
            date = selectedDate.timeInMillis,
            startTime = startTime,
            endTime = endTime,
            userId = currentUserId,
            notes = notes,
            photoPath = photoPath
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.expenseDao().insertExpense(expense)
            android.util.Log.d("AddExpenseFragment", "Saved expense: $description, amount: $amount")
            Toast.makeText(requireContext(), "Expense saved successfully!", Toast.LENGTH_SHORT).show()
            clearForm()
        }
    }

    private fun clearForm() {
        etAmount.text?.clear()
        etDescription.text?.clear()
        etCategory.text?.clear()
        etStartTime.text?.clear()
        etEndTime.text?.clear()
        etNotes.text?.clear()
        ivPhotoPreview.visibility = View.GONE
        photoUri = null
        photoPath = null
        selectedCategoryId = 0
        startTime = ""
        endTime = ""
        selectedDate = Calendar.getInstance()
        etDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }
}
