package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.finflow.app.R
import com.finflow.app.data.firebase.FirebaseRepository
import com.finflow.app.data.local.database.AppDatabase
import com.finflow.app.data.local.entities.Achievement
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * AchievementsFragment - Own Feature #1
 *
 * Automatically awards achievements to the user based on their spending behaviour:
 *  - "First Expense"   : awarded when the user logs their first ever expense
 *  - "Budget Setter"   : awarded when the user sets their first budget goal
 *  - "Category Creator": awarded when the user creates 3+ categories
 *  - "Saver"           : awarded when total spending this month is under their maximum goal
 *
 * Achievements are stored both locally in RoomDB and mirrored to Firestore.
 * The screen displays all unlocked achievements with points and unlock dates.
 */
class AchievementsFragment : Fragment() {

    private val TAG = "AchievementsFragment"
    private var currentUserId: Long = 1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_achievements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentUserId()
        Log.d(TAG, "AchievementsFragment loaded for userId=$currentUserId")

        checkAndAwardAchievements()
        loadAchievements(view)
    }

    private fun loadCurrentUserId() {
        val sharedPref = requireContext().getSharedPreferences("finflow_prefs", Activity.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("current_user_id", 1L)
    }

    /**
     * Checks current user activity and awards achievements they have not yet earned.
     * Awards are stored locally and mirrored to Firestore.
     */
    private fun checkAndAwardAchievements() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val firebaseRepo = FirebaseRepository()
                val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                // Achievement: First Expense
                val expenseCount = db.expenseDao().getExpenseCountInRange(currentUserId, 0L, System.currentTimeMillis())
                if (expenseCount >= 1 && db.achievementDao().getAchievementByType(currentUserId, "FIRST_EXPENSE") == null) {
                    val ach = Achievement(
                        userId = currentUserId,
                        achievementType = "FIRST_EXPENSE",
                        title = "First Expense",
                        description = "You logged your very first expense!",
                        pointsAwarded = 10
                    )
                    val id = db.achievementDao().insertAchievement(ach)
                    firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                    Log.d(TAG, "Achievement awarded: FIRST_EXPENSE")
                }

                // Achievement: Budget Setter
                val budgets = db.budgetDao().getBudgetsForMonthList(currentUserId, monthYear)
                if (budgets.isNotEmpty() && db.achievementDao().getAchievementByType(currentUserId, "BUDGET_SETTER") == null) {
                    val ach = Achievement(
                        userId = currentUserId,
                        achievementType = "BUDGET_SETTER",
                        title = "Budget Setter",
                        description = "You set your first monthly budget goal!",
                        pointsAwarded = 15
                    )
                    val id = db.achievementDao().insertAchievement(ach)
                    firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                    Log.d(TAG, "Achievement awarded: BUDGET_SETTER")
                }

                // Achievement: Category Creator (3+ categories)
                val categoryCount = db.categoryDao().getCategoryCount(currentUserId)
                if (categoryCount >= 3 && db.achievementDao().getAchievementByType(currentUserId, "CATEGORY_CREATOR") == null) {
                    val ach = Achievement(
                        userId = currentUserId,
                        achievementType = "CATEGORY_CREATOR",
                        title = "Category Creator",
                        description = "You created 3 or more expense categories!",
                        pointsAwarded = 20
                    )
                    val id = db.achievementDao().insertAchievement(ach)
                    firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                    Log.d(TAG, "Achievement awarded: CATEGORY_CREATOR")
                }

                // Achievement: Saver (spending under max goal this month)
                if (budgets.isNotEmpty()) {
                    val startOfMonth = getStartOfMonth()
                    val endOfMonth = getEndOfMonth()
                    val totalSpent = db.expenseDao().getTotalSpentInRange(currentUserId, startOfMonth, endOfMonth) ?: 0.0
                    val totalMaxGoal = budgets.sumOf { it.maxGoal }

                    if (totalMaxGoal > 0 && totalSpent <= totalMaxGoal &&
                        db.achievementDao().getAchievementByType(currentUserId, "SAVER_${monthYear}") == null
                    ) {
                        val ach = Achievement(
                            userId = currentUserId,
                            achievementType = "SAVER_${monthYear}",
                            title = "Under Budget",
                            description = "You stayed under your maximum spending goal this month!",
                            pointsAwarded = 50
                        )
                        val id = db.achievementDao().insertAchievement(ach)
                        firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                        Log.d(TAG, "Achievement awarded: SAVER for $monthYear")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking achievements: ${e.message}")
            }
        }
    }

    /** Loads all achievements from RoomDB and displays them in a RecyclerView. */
    private fun loadAchievements(view: View) {
        val rvAchievements = view.findViewById<RecyclerView>(R.id.rv_achievements)
        val tvTotalPoints = view.findViewById<TextView>(R.id.tv_total_points)

        rvAchievements.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            db.achievementDao().getAllAchievements(currentUserId).collect { achievements ->
                Log.d(TAG, "Loaded ${achievements.size} achievements")
                rvAchievements.adapter = AchievementAdapter(achievements)

                val total = achievements.sumOf { it.pointsAwarded }
                tvTotalPoints.text = "Total Points: $total"
            }
        }
    }

    private fun achievementToMap(ach: Achievement): Map<String, Any> = mapOf(
        "id" to ach.id,
        "userId" to ach.userId,
        "achievementType" to ach.achievementType,
        "title" to ach.title,
        "description" to ach.description,
        "pointsAwarded" to ach.pointsAwarded,
        "unlockedAt" to ach.unlockedAt
    )

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    /** RecyclerView adapter for achievement items. */
    private class AchievementAdapter(private val items: List<Achievement>) :
        RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tv_achievement_title)
            val tvDescription: TextView = view.findViewById(R.id.tv_achievement_description)
            val tvPoints: TextView = view.findViewById(R.id.tv_achievement_points)
            val tvDate: TextView = view.findViewById(R.id.tv_achievement_date)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_achievement, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ach = items[position]
            holder.tvTitle.text = "🏆 ${ach.title}"
            holder.tvDescription.text = ach.description
            holder.tvPoints.text = "+${ach.pointsAwarded} pts"
            holder.tvDate.text = "Unlocked: ${dateFormat.format(Date(ach.unlockedAt))}"
        }

        override fun getItemCount() = items.size
    }
}
