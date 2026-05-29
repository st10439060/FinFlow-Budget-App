package com.finflow.app.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
 * Awards achievements automatically based on spending behaviour.
 * Displays: unlocked badges with icons, total points, level with progress bar,
 * and a consecutive-day streak counter.
 */
class AchievementsFragment : Fragment() {

    private val TAG = "AchievementsFragment"
    private var currentUserId: Long = 1L

    // Points required to reach each level (index = level - 1)
    private val levelThresholds = listOf(0, 50, 150, 300, 500, 800, 1200)
    private val levelNames = listOf("Beginner", "Saver", "Tracker", "Planner", "Expert", "Master", "Legend")

    private val badgeIconMap = mapOf(
        "FIRST_EXPENSE" to "💸",
        "BUDGET_SETTER" to "🎯",
        "CATEGORY_CREATOR" to "🗂️",
        "SAVER" to "💰"
    )

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

    private fun checkAndAwardAchievements() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val firebaseRepo = FirebaseRepository()
                val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                val expenseCount = db.expenseDao().getExpenseCountInRange(currentUserId, 0L, System.currentTimeMillis())
                if (expenseCount >= 1 && db.achievementDao().getAchievementByType(currentUserId, "FIRST_EXPENSE") == null) {
                    val ach = Achievement(userId = currentUserId, achievementType = "FIRST_EXPENSE",
                        title = "First Expense", description = "You logged your very first expense!", pointsAwarded = 10)
                    val id = db.achievementDao().insertAchievement(ach)
                    firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                }

                val budgets = db.budgetDao().getBudgetsForMonthList(currentUserId, monthYear)
                if (budgets.isNotEmpty() && db.achievementDao().getAchievementByType(currentUserId, "BUDGET_SETTER") == null) {
                    val ach = Achievement(userId = currentUserId, achievementType = "BUDGET_SETTER",
                        title = "Budget Setter", description = "You set your first monthly budget goal!", pointsAwarded = 15)
                    val id = db.achievementDao().insertAchievement(ach)
                    firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                }

                val categoryCount = db.categoryDao().getCategoryCount(currentUserId)
                if (categoryCount >= 3 && db.achievementDao().getAchievementByType(currentUserId, "CATEGORY_CREATOR") == null) {
                    val ach = Achievement(userId = currentUserId, achievementType = "CATEGORY_CREATOR",
                        title = "Category Creator", description = "You created 3 or more expense categories!", pointsAwarded = 20)
                    val id = db.achievementDao().insertAchievement(ach)
                    firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                }

                if (budgets.isNotEmpty()) {
                    val startOfMonth = getStartOfMonth()
                    val endOfMonth = getEndOfMonth()
                    val totalSpent = db.expenseDao().getTotalSpentInRange(currentUserId, startOfMonth, endOfMonth) ?: 0.0
                    val totalMaxGoal = budgets.sumOf { it.maxGoal }
                    if (totalMaxGoal > 0 && totalSpent <= totalMaxGoal &&
                        db.achievementDao().getAchievementByType(currentUserId, "SAVER_${monthYear}") == null) {
                        val ach = Achievement(userId = currentUserId, achievementType = "SAVER_${monthYear}",
                            title = "Under Budget", description = "You stayed under your maximum spending goal this month!", pointsAwarded = 50)
                        val id = db.achievementDao().insertAchievement(ach)
                        firebaseRepo.saveAchievement(currentUserId, id, achievementToMap(ach.copy(id = id)))
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking achievements: ${e.message}")
            }
        }
    }

    private fun loadAchievements(view: View) {
        val rvAchievements = view.findViewById<RecyclerView>(R.id.rv_achievements)
        val tvTotalPoints = view.findViewById<TextView>(R.id.tv_total_points)
        val tvLevelLabel = view.findViewById<TextView>(R.id.tv_level_label)
        val tvNextLevelPts = view.findViewById<TextView>(R.id.tv_next_level_pts)
        val progressLevel = view.findViewById<ProgressBar>(R.id.progress_level)
        val tvStreakCount = view.findViewById<TextView>(R.id.tv_streak_count)

        rvAchievements.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())

            // Calculate streak
            val streak = calculateStreak(db)
            tvStreakCount.text = streak.toString()

            db.achievementDao().getAllAchievements(currentUserId).collect { achievements ->
                Log.d(TAG, "Loaded ${achievements.size} achievements")
                rvAchievements.adapter = AchievementAdapter(achievements, badgeIconMap)

                val total = achievements.sumOf { it.pointsAwarded }
                tvTotalPoints.text = "$total pts"

                val (level, levelName, progressPct, ptsToNext) = computeLevel(total)
                tvLevelLabel.text = "Level $level — $levelName"
                progressLevel.progress = progressPct
                tvNextLevelPts.text = if (ptsToNext > 0) "$ptsToNext pts to next level" else "Max level reached!"
            }
        }
    }

    /** Counts how many consecutive days (ending today) the user logged at least one expense. */
    private suspend fun calculateStreak(db: AppDatabase): Int {
        val allExpenses = db.expenseDao().getExpensesByDateRange(
            currentUserId, 0L, System.currentTimeMillis()
        )
        if (allExpenses.isEmpty()) return 0

        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expenseDays = allExpenses.map { dayFormat.format(Date(it.date)) }.toSet()

        var streak = 0
        val cal = Calendar.getInstance()
        while (true) {
            val dayStr = dayFormat.format(cal.time)
            if (dayStr in expenseDays) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    data class LevelInfo(val level: Int, val name: String, val progressPct: Int, val ptsToNext: Int)

    private fun computeLevel(totalPoints: Int): LevelInfo {
        var currentLevel = 1
        for (i in levelThresholds.indices) {
            if (totalPoints >= levelThresholds[i]) currentLevel = i + 1
        }
        currentLevel = currentLevel.coerceAtMost(levelThresholds.size)
        val levelName = levelNames.getOrElse(currentLevel - 1) { "Legend" }

        val currentThreshold = levelThresholds.getOrElse(currentLevel - 1) { 0 }
        val nextThreshold = levelThresholds.getOrNull(currentLevel)

        val progressPct: Int
        val ptsToNext: Int
        if (nextThreshold == null) {
            progressPct = 100
            ptsToNext = 0
        } else {
            val range = (nextThreshold - currentThreshold).toFloat()
            val earned = (totalPoints - currentThreshold).toFloat()
            progressPct = ((earned / range) * 100).toInt().coerceIn(0, 100)
            ptsToNext = nextThreshold - totalPoints
        }

        return LevelInfo(currentLevel, levelName, progressPct, ptsToNext)
    }

    private fun achievementToMap(ach: Achievement): Map<String, Any> = mapOf(
        "id" to ach.id, "userId" to ach.userId, "achievementType" to ach.achievementType,
        "title" to ach.title, "description" to ach.description,
        "pointsAwarded" to ach.pointsAwarded, "unlockedAt" to ach.unlockedAt
    )

    private fun getStartOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    private class AchievementAdapter(
        private val items: List<Achievement>,
        private val badgeIconMap: Map<String, String>
    ) : RecyclerView.Adapter<AchievementAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvBadgeIcon: TextView = view.findViewById(R.id.tv_badge_icon)
            val tvTitle: TextView = view.findViewById(R.id.tv_achievement_title)
            val tvDescription: TextView = view.findViewById(R.id.tv_achievement_description)
            val tvPoints: TextView = view.findViewById(R.id.tv_achievement_points)
            val tvDate: TextView = view.findViewById(R.id.tv_achievement_date)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_achievement, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ach = items[position]
            val typeKey = ach.achievementType.substringBefore("_2")
            val icon = badgeIconMap[typeKey] ?: badgeIconMap[ach.achievementType] ?: "🏆"
            holder.tvBadgeIcon.text = icon
            holder.tvTitle.text = ach.title
            holder.tvDescription.text = ach.description
            holder.tvPoints.text = "+${ach.pointsAwarded} pts"
            holder.tvDate.text = "Unlocked: ${dateFormat.format(Date(ach.unlockedAt))}"
        }

        override fun getItemCount() = items.size
    }
}
