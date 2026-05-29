package com.finflow.app.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * FirebaseRepository - handles all Firestore read/write operations.
 *
 * Firestore structure:
 *   users/{userId}/expenses/{expenseId}
 *   users/{userId}/categories/{categoryId}
 *   users/{userId}/budgets/{budgetId}
 *   users/{userId}/achievements/{achievementId}
 *
 * Uses the local Room userId (Long) converted to String as the Firestore document path.
 * All methods suspend so callers use coroutines.
 */
class FirebaseRepository {

    private val TAG = "FirebaseRepository"
    private val db: FirebaseFirestore = Firebase.firestore

    // ─── Expenses ────────────────────────────────────────────────────────────

    /** Saves a new expense document to Firestore under users/{userId}/expenses. */
    suspend fun saveExpense(userId: Long, expenseId: Long, data: Map<String, Any>) {
        try {
            db.collection("users")
                .document(userId.toString())
                .collection("expenses")
                .document(expenseId.toString())
                .set(data)
                .await()
            Log.d(TAG, "Expense $expenseId saved to Firestore for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving expense to Firestore: ${e.message}")
        }
    }

    /** Deletes an expense document from Firestore. */
    suspend fun deleteExpense(userId: Long, expenseId: Long) {
        try {
            db.collection("users")
                .document(userId.toString())
                .collection("expenses")
                .document(expenseId.toString())
                .delete()
                .await()
            Log.d(TAG, "Expense $expenseId deleted from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expense from Firestore: ${e.message}")
        }
    }

    /** Fetches all expense documents for a user from Firestore. */
    suspend fun getExpenses(userId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users")
                .document(userId.toString())
                .collection("expenses")
                .get()
                .await()
            Log.d(TAG, "Fetched ${snapshot.size()} expenses from Firestore for user $userId")
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching expenses from Firestore: ${e.message}")
            emptyList()
        }
    }

    // ─── Categories ──────────────────────────────────────────────────────────

    /** Saves a category document to Firestore. */
    suspend fun saveCategory(userId: Long, categoryId: Long, data: Map<String, Any>) {
        try {
            db.collection("users")
                .document(userId.toString())
                .collection("categories")
                .document(categoryId.toString())
                .set(data)
                .await()
            Log.d(TAG, "Category $categoryId saved to Firestore for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving category to Firestore: ${e.message}")
        }
    }

    /** Deletes a category document from Firestore. */
    suspend fun deleteCategory(userId: Long, categoryId: Long) {
        try {
            db.collection("users")
                .document(userId.toString())
                .collection("categories")
                .document(categoryId.toString())
                .delete()
                .await()
            Log.d(TAG, "Category $categoryId deleted from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting category from Firestore: ${e.message}")
        }
    }

    // ─── Budgets / Goals ─────────────────────────────────────────────────────

    /** Saves a budget/goal document to Firestore. */
    suspend fun saveBudget(userId: Long, budgetId: Long, data: Map<String, Any>) {
        try {
            db.collection("users")
                .document(userId.toString())
                .collection("budgets")
                .document(budgetId.toString())
                .set(data)
                .await()
            Log.d(TAG, "Budget $budgetId saved to Firestore for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving budget to Firestore: ${e.message}")
        }
    }

    /** Fetches all budget documents for a user from Firestore. */
    suspend fun getBudgets(userId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users")
                .document(userId.toString())
                .collection("budgets")
                .get()
                .await()
            Log.d(TAG, "Fetched ${snapshot.size()} budgets from Firestore for user $userId")
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching budgets from Firestore: ${e.message}")
            emptyList()
        }
    }

    // ─── Achievements ─────────────────────────────────────────────────────────

    /** Saves an achievement document to Firestore. */
    suspend fun saveAchievement(userId: Long, achievementId: Long, data: Map<String, Any>) {
        try {
            db.collection("users")
                .document(userId.toString())
                .collection("achievements")
                .document(achievementId.toString())
                .set(data)
                .await()
            Log.d(TAG, "Achievement $achievementId saved to Firestore for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving achievement to Firestore: ${e.message}")
        }
    }

    /** Fetches all achievement documents for a user from Firestore. */
    suspend fun getAchievements(userId: Long): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users")
                .document(userId.toString())
                .collection("achievements")
                .get()
                .await()
            Log.d(TAG, "Fetched ${snapshot.size()} achievements from Firestore for user $userId")
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching achievements from Firestore: ${e.message}")
            emptyList()
        }
    }
}
