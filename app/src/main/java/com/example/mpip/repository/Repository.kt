package com.example.mpip.repository

import android.util.Log
import com.example.mpip.domain.DailyProgress
import com.example.mpip.domain.DailyQuestionnaire
import com.example.mpip.domain.DailyTask
import com.example.mpip.domain.DiaryEntry
import com.example.mpip.domain.DiaryFilter
import com.example.mpip.domain.DiaryStats
import com.example.mpip.domain.Emotion
import com.example.mpip.domain.EmotionRelation
import com.example.mpip.domain.MonthlyStats
import com.example.mpip.domain.TaskHistoryFilter
import com.example.mpip.domain.TaskHistoryItem
import com.example.mpip.domain.TaskHistoryStats
import com.example.mpip.domain.TaskTemplate
import com.example.mpip.domain.UserProgress
import com.example.mpip.domain.enums.PetActionType
import com.example.mpip.domain.enums.diary.DiarySortOption
import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.domain.mentalHealthTips.MentalHealthTip
import com.example.mpip.domain.mentalHealthTips.TipGenerationRequest
import com.example.mpip.domain.mentalHealthTips.TipPreferences
import com.example.mpip.service.OpenAIService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

class Repository {
    private val database =
        FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val emotionsRef = database.getReference("emotions")
    private val emotionRelationsRef = database.getReference("emotion_relations")
    private val taskTemplatesRef = database.getReference("task_templates")
    private val dailyQuestionnairesRef = database.getReference("daily_questionnaires")
    private val dailyTasksRef = database.getReference("daily_tasks")
    private val userProgressRef = database.getReference("user_progress")
    private val diaryEntriesRef = database.getReference("diary_entries")
    private val mentalHealthTipsRef = database.getReference("mental_health_tips")
    private val openAIService = OpenAIService()

    suspend fun getActiveEmotions(): List<Emotion> {
        return suspendCancellableCoroutine { continuation ->
            emotionsRef.orderByChild("active").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val emotions = mutableListOf<Emotion>()

                        for (child in snapshot.children) {
                            val emotion = child.getValue(Emotion::class.java)
                            if (emotion != null) {
                                emotions.add(emotion.copy(id = child.key ?: ""))
                            }
                        }
                        emotions.sortWith(compareBy({ it.category }, { it.name }))
                        continuation.resume(emotions)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun getEmotionRelations(): List<EmotionRelation> {
        return suspendCancellableCoroutine { continuation ->
            emotionRelationsRef.orderByChild("active").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val relations = mutableListOf<EmotionRelation>()

                        for (child in snapshot.children) {
                            val relation = child.getValue(EmotionRelation::class.java)
                            if (relation != null) {
                                relations.add(relation.copy(id = child.key ?: ""))
                            }
                        }

                        relations.sortBy { it.name }
                        continuation.resume(relations)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun saveDailyQuestionnaire(questionnaire: DailyQuestionnaire): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val docId = "${questionnaire.userId}_${questionnaire.date}"

            val enhancedQuestionnaire = questionnaire.copy(
                id = docId,
                completed = true,
                completedAt = System.currentTimeMillis()
            )

            Log.d("RepositoryDebug", "Saving questionnaire with completed=true: $docId")
            dailyQuestionnairesRef.child(docId)
                .setValue(enhancedQuestionnaire)
                .addOnSuccessListener {
                    Log.d("RepositoryDebug", "✅ Questionnaire saved successfully: $docId")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    Log.e("RepositoryDebug", "❌ Failed to save questionnaire: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    suspend fun hasCompletedTodayQuestionnaire(userId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val today = dateFormat.format(Date())
            val docId = "${userId}_${today}"

            Log.d("RepositoryDebug", "Checking questionnaire completion for: $docId")

            dailyQuestionnairesRef.child(docId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val questionnaire = snapshot.getValue(DailyQuestionnaire::class.java)
                            val isCompleted = questionnaire?.completed == true
                            Log.d("RepositoryDebug", "Questionnaire found: completed=$isCompleted")
                            continuation.resume(isCompleted)
                        } else {
                            Log.d("RepositoryDebug", "No questionnaire found for today")
                            continuation.resume(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error checking questionnaire: ${error.message}")
                        continuation.resume(false)
                    }
                })
        }
    }

    suspend fun getTaskTemplatesForEmotions(emotionIds: List<String>): List<TaskTemplate> {
        if (emotionIds.isEmpty()) {
            Log.d("RepositoryDebug", "No emotion IDs provided for task filtering")
            return emptyList()
        }

        return suspendCancellableCoroutine { continuation ->
            Log.d(
                "RepositoryDebug",
                "=== GETTING QUESTIONNAIRE TASKS FOR EMOTIONS: $emotionIds ==="
            )

            taskTemplatesRef
                .orderByChild("category")
                .equalTo(TaskCategory.QUESTIONNAIRE_BASED.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val templates = mutableListOf<TaskTemplate>()

                        for (child in snapshot.children) {
                            try {
                                val template = child.getValue(TaskTemplate::class.java)
                                if (template != null && template.isActive) {
                                    val templateWithId = template.copy(id = child.key ?: "")

                                    val hasMatchingEmotion =
                                        template.triggerEmotions.any { triggerId ->
                                            emotionIds.contains(triggerId)
                                        }

                                    if (hasMatchingEmotion || template.triggerEmotions.isEmpty()) {
                                        templates.add(templateWithId)
                                        Log.d(
                                            "RepositoryDebug",
                                            "Added template: ${template.title} (triggers: ${template.triggerEmotions})"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("RepositoryDebug", "Error parsing template: ${e.message}")
                            }
                        }

                        Log.d(
                            "RepositoryDebug",
                            "Found ${templates.size} matching questionnaire templates"
                        )
                        continuation.resume(templates)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "RepositoryDebug",
                            "Error fetching questionnaire templates: ${error.message}"
                        )
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun getActiveDailyRoutineTasks(): List<TaskTemplate> {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "=== GETTING ACTIVE DAILY ROUTINE TASKS ===")

            taskTemplatesRef
                .orderByChild("category")
                .equalTo(TaskCategory.DAILY_ROUTINE.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tasks = mutableListOf<TaskTemplate>()

                        for (child in snapshot.children) {
                            try {
                                val task = child.getValue(TaskTemplate::class.java)
                                if (task != null && task.isActive) {
                                    tasks.add(task.copy(id = child.key ?: ""))
                                    Log.d("RepositoryDebug", "Added routine task: ${task.title}")
                                }
                            } catch (e: Exception) {
                                Log.e("RepositoryDebug", "Error parsing routine task: ${e.message}")
                            }
                        }

                        Log.d("RepositoryDebug", "Found ${tasks.size} active routine tasks")
                        continuation.resume(tasks)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error fetching routine tasks: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun hasDailyRoutineTasksForToday(userId: String): Boolean {
        val today = dateFormat.format(Date())

        return suspendCancellableCoroutine { continuation ->
            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var hasDailyTasks = false

                        for (child in snapshot.children) {
                            val task = child.getValue(DailyTask::class.java)
                            if (task != null &&
                                task.date == today &&
                                task.category == TaskCategory.DAILY_ROUTINE
                            ) {
                                hasDailyTasks = true
                                break
                            }
                        }

                        continuation.resume(hasDailyTasks)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(false)
                    }
                })
        }
    }

    suspend fun saveDailyRoutineTasks(tasks: List<DailyTask>): Boolean {
        return suspendCancellableCoroutine { continuation ->
            if (tasks.isEmpty()) {
                continuation.resume(true)
                return@suspendCancellableCoroutine
            }

            val updates = mutableMapOf<String, Any>()
            tasks.forEach { task ->
                updates["daily_tasks/${task.id}"] = task
                Log.d("RepositoryDebug", "Saving daily routine task: ${task.id} - ${task.title}")
            }

            database.reference.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("RepositoryDebug", "Successfully saved ${tasks.size} daily routine tasks")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    Log.e("RepositoryDebug", "Failed to save daily routine tasks: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    suspend fun getLimitedTaskTemplatesForEmotions(
        emotionIds: List<String>,
        limit: Int = 3
    ): List<TaskTemplate> {
        val allMatching = getTaskTemplatesForEmotions(emotionIds)
        return allMatching.shuffled().take(limit)
    }

    suspend fun saveDailyTasks(tasks: List<DailyTask>): Boolean {
        return suspendCancellableCoroutine { continuation ->
            if (tasks.isEmpty()) {
                Log.d("RepositoryDebug", "No tasks to save")
                continuation.resume(true)
                return@suspendCancellableCoroutine
            }

            val userId = tasks.first().userId
            val today = dateFormat.format(Date())
            val taskCategory =
                tasks.first().category

            Log.d(
                "RepositoryDebug",
                "=== SAVING ${tasks.size} ${taskCategory} TASKS FOR $userId ==="
            )

            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val existingTasksOfSameType = mutableListOf<DailyTask>()

                        for (child in snapshot.children) {
                            val task = child.getValue(DailyTask::class.java)
                            if (task != null &&
                                task.date == today &&
                                task.category == taskCategory
                            ) { // Only check same category
                                existingTasksOfSameType.add(task)
                            }
                        }

                        if (existingTasksOfSameType.isNotEmpty()) {
                            Log.w(
                                "RepositoryDebug",
                                "${taskCategory} tasks already exist for today (${existingTasksOfSameType.size}), skipping save"
                            )
                            continuation.resume(true)
                            return
                        }

                        val updates = mutableMapOf<String, Any>()
                        tasks.forEach { task ->
                            updates["daily_tasks/${task.id}"] = task
                            Log.d(
                                "RepositoryDebug",
                                "Adding ${taskCategory} task to save: ${task.id} - ${task.title}"
                            )
                        }

                        Log.d("RepositoryDebug", "Saving ${updates.size} updates to Firebase...")

                        database.reference.updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d(
                                    "RepositoryDebug",
                                    "✅ Successfully saved ${tasks.size} ${taskCategory} tasks"
                                )
                                continuation.resume(true)
                            }
                            .addOnFailureListener { error ->
                                Log.e(
                                    "RepositoryDebug",
                                    "❌ Failed to save ${taskCategory} tasks: ${error.message}"
                                )
                                continuation.resume(false)
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "RepositoryDebug",
                            "Error checking existing ${taskCategory} tasks: ${error.message}"
                        )
                        continuation.resume(false)
                    }
                })
        }
    }

    suspend fun getTodayTasks(userId: String): List<DailyTask> {
        return suspendCancellableCoroutine { continuation ->
            val today = dateFormat.format(Date())

            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tasks = mutableListOf<DailyTask>()

                        for (child in snapshot.children) {
                            val task = child.getValue(DailyTask::class.java)
                            if (task != null && task.date == today) {
                                tasks.add(task.copy(id = child.key ?: ""))
                            }
                        }

                        tasks.sortWith(compareBy<DailyTask> { it.category.ordinal }
                            .thenByDescending { it.points })

                        continuation.resume(tasks)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun completeTask(
        taskId: String,
        userResponse: String = "",
        photoPath: String = ""
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val updates = mutableMapOf<String, Any>(
                "daily_tasks/$taskId/completed" to true,
                "daily_tasks/$taskId/completedAt" to System.currentTimeMillis()
            )

            if (userResponse.isNotEmpty()) {
                updates["daily_tasks/$taskId/userResponse"] = userResponse
                Log.d("RepositoryDebug", "Saving user response for task $taskId")
            }

            if (photoPath.isNotEmpty()) {
                updates["daily_tasks/$taskId/photoPath"] = photoPath
                Log.d("RepositoryDebug", "Saving photo path for task $taskId")
            }

            Log.d("RepositoryDebug", "Updating task completion: $taskId")

            database.reference.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("RepositoryDebug", "✅ Task $taskId marked as completed")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    Log.e("RepositoryDebug", "❌ Failed to complete task $taskId: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    suspend fun getUserProgress(userId: String): UserProgress? {
        return suspendCancellableCoroutine { continuation ->
            userProgressRef.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val progress = snapshot.getValue(UserProgress::class.java)
                            continuation.resume(progress)
                        } else {
                            val initialProgress = UserProgress(userId = userId)
                            userProgressRef.child(userId).setValue(initialProgress)
                                .addOnSuccessListener {
                                    continuation.resume(initialProgress)
                                }
                                .addOnFailureListener {
                                    continuation.resume(null)
                                }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(null)
                    }
                })
        }
    }

    suspend fun updateUserProgress(userId: String, pointsEarned: Int): Boolean {
        return suspendCancellableCoroutine { continuation ->
            userProgressRef.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentProgress = if (snapshot.exists()) {
                            snapshot.getValue(UserProgress::class.java)
                                ?: UserProgress(userId = userId)
                        } else {
                            UserProgress(userId = userId)
                        }

                        val today = dateFormat.format(Date())

                        val updatedProgress = currentProgress.copy(
                            totalPoints = currentProgress.totalPoints + pointsEarned,
                            tasksCompleted = currentProgress.tasksCompleted + 1,
                            lastActiveDate = today,
                            currentStreak = if (isConsecutiveDay(
                                    currentProgress.lastActiveDate,
                                    today
                                )
                            ) {
                                currentProgress.currentStreak + 1
                            } else {
                                1
                            },
                            level = calculateLevel(currentProgress.totalPoints + pointsEarned)
                        ).let { progress ->
                            progress.copy(
                                longestStreak = maxOf(
                                    progress.longestStreak,
                                    progress.currentStreak
                                )
                            )
                        }

                        userProgressRef.child(userId).setValue(updatedProgress)
                            .addOnSuccessListener {
                                continuation.resume(true)
                            }
                            .addOnFailureListener {
                                continuation.resume(false)
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(false)
                    }
                })
        }
    }

    private fun isConsecutiveDay(lastDate: String, currentDate: String): Boolean {
        if (lastDate.isEmpty()) return false

        return try {
            val last = dateFormat.parse(lastDate)
            val current = dateFormat.parse(currentDate)

            if (last != null && current != null) {
                val diffInMillis = current.time - last.time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
                diffInDays == 1L
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateUserProgressWithPoints(
        userId: String,
        pointsEarned: Int,
        taskCompleted: Boolean = true
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            userProgressRef.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentProgress = if (snapshot.exists()) {
                            snapshot.getValue(UserProgress::class.java)
                                ?: UserProgress(userId = userId)
                        } else {
                            UserProgress(userId = userId)
                        }

                        val today = dateFormat.format(Date())
                        val currentMonth = today.substring(0, 7)

                        val newStreak =
                            if (isConsecutiveDay(currentProgress.lastActiveDate, today)) {
                                currentProgress.currentStreak + 1
                            } else if (currentProgress.lastActiveDate != today) {
                                1
                            } else {
                                currentProgress.currentStreak
                            }

                        val streakBonus = if (currentProgress.lastActiveDate != today) {
                            minOf(newStreak, 10)
                        } else 0

                        val totalPointsToAdd = pointsEarned + streakBonus

                        val currentMonthStats =
                            currentProgress.monthlyStats[currentMonth] ?: MonthlyStats(
                                month = currentMonth,
                                totalPoints = 0,
                                tasksCompleted = 0,
                                loginDays = emptyList(),
                                streakDays = 0,
                                pointsSpent = 0
                            )

                        val updatedMonthStats = currentMonthStats.copy(
                            totalPoints = currentMonthStats.totalPoints + totalPointsToAdd,
                            tasksCompleted = if (taskCompleted) currentMonthStats.tasksCompleted + 1 else currentMonthStats.tasksCompleted,
                            loginDays = if (currentMonthStats.loginDays.contains(today)) {
                                currentMonthStats.loginDays
                            } else {
                                currentMonthStats.loginDays + today
                            },
                            streakDays = newStreak
                        )

                        val updatedProgress = currentProgress.copy(
                            totalPoints = currentProgress.totalPoints + totalPointsToAdd,
                            availablePoints = currentProgress.availablePoints + totalPointsToAdd,
                            tasksCompleted = if (taskCompleted) currentProgress.tasksCompleted + 1 else currentProgress.tasksCompleted,
                            lastActiveDate = today,
                            currentStreak = newStreak,
                            longestStreak = maxOf(currentProgress.longestStreak, newStreak),
                            level = calculateLevel(currentProgress.totalPoints + totalPointsToAdd),
                            monthlyStats = currentProgress.monthlyStats + (currentMonth to updatedMonthStats),
                            firstLoginDate = if (currentProgress.firstLoginDate.isEmpty()) today else currentProgress.firstLoginDate
                        )

                        userProgressRef.child(userId).setValue(updatedProgress)
                            .addOnSuccessListener {
                                Log.d(
                                    "RepositoryDebug",
                                    "✅ User progress updated: +$totalPointsToAdd points (task: $pointsEarned, streak: $streakBonus)"
                                )
                                continuation.resume(true)
                            }
                            .addOnFailureListener { error ->
                                Log.e(
                                    "RepositoryDebug",
                                    "❌ Failed to update user progress: ${error.message}"
                                )
                                continuation.resume(false)
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error updating user progress: ${error.message}")
                        continuation.resume(false)
                    }
                })
        }
    }

    suspend fun spendPointsOnPetAction(userId: String, action: PetActionType): Boolean {
        return suspendCancellableCoroutine { continuation ->
            userProgressRef.child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentProgress = if (snapshot.exists()) {
                            snapshot.getValue(UserProgress::class.java)
                                ?: UserProgress(userId = userId)
                        } else {
                            UserProgress(userId = userId)
                        }

                        if (currentProgress.availablePoints < action.pointsCost) {
                            Log.d(
                                "RepositoryDebug",
                                "❌ Insufficient points: has ${currentProgress.availablePoints}, needs ${action.pointsCost}"
                            )
                            continuation.resume(false)
                            return
                        }

                        val today = dateFormat.format(Date())
                        val currentMonth = today.substring(0, 7)

                        val currentMonthStats =
                            currentProgress.monthlyStats[currentMonth] ?: MonthlyStats(
                                month = currentMonth,
                                pointsSpent = 0
                            )

                        val updatedMonthStats = currentMonthStats.copy(
                            pointsSpent = currentMonthStats.pointsSpent + action.pointsCost
                        )

                        val updatedProgress = currentProgress.copy(
                            availablePoints = currentProgress.availablePoints - action.pointsCost,
                            totalPointsSpent = currentProgress.totalPointsSpent + action.pointsCost,
                            petInteractions = currentProgress.petInteractions + 1,
                            monthlyStats = currentProgress.monthlyStats + (currentMonth to updatedMonthStats)
                        )

                        userProgressRef.child(userId).setValue(updatedProgress)
                            .addOnSuccessListener {
                                Log.d(
                                    "RepositoryDebug",
                                    "✅ Spent ${action.pointsCost} points on ${action.displayName}"
                                )
                                continuation.resume(true)
                            }
                            .addOnFailureListener { error ->
                                Log.e(
                                    "RepositoryDebug",
                                    "❌ Failed to spend points: ${error.message}"
                                )
                                continuation.resume(false)
                            }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error spending points: ${error.message}")
                        continuation.resume(false)
                    }
                })
        }
    }


    private fun calculateLevel(totalPoints: Int): Int {
        return when {
            totalPoints >= 5000 -> 10
            totalPoints >= 3000 -> 9
            totalPoints >= 2000 -> 8
            totalPoints >= 1500 -> 7
            totalPoints >= 1000 -> 6
            totalPoints >= 750 -> 5
            totalPoints >= 500 -> 4
            totalPoints >= 250 -> 3
            totalPoints >= 100 -> 2
            else -> 1
        }
    }

    suspend fun getQuestionnaireTaskHistory(
        userId: String,
        filter: TaskHistoryFilter = TaskHistoryFilter(),
        limit: Int = 50
    ): List<TaskHistoryItem> {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "=== GETTING QUESTIONNAIRE TASK HISTORY ===")

            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val historyItems = mutableListOf<TaskHistoryItem>()

                        for (child in snapshot.children) {
                            try {
                                val task = child.getValue(DailyTask::class.java)
                                if (task != null &&
                                    task.completed &&
                                    task.category == TaskCategory.QUESTIONNAIRE_BASED
                                ) {

                                    if (filter.startDate.isNotEmpty() && task.date < filter.startDate) continue
                                    if (filter.endDate.isNotEmpty() && task.date > filter.endDate) continue

                                    if (task.points < filter.minPoints || task.points > filter.maxPoints) continue

                                    if (filter.emotions.isNotEmpty() &&
                                        !task.triggeringEmotionNames.any {
                                            filter.emotions.contains(
                                                it
                                            )
                                        }
                                    ) continue

                                    val completionTime = if (task.completedAt > 0) {
                                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(
                                            Date(
                                                task.completedAt
                                            )
                                        )
                                    } else "Unknown"

                                    val historyItem = TaskHistoryItem(
                                        task = task.copy(id = child.key ?: ""),
                                        questionnaire = null, // Will be populated separately if needed
                                        completionDate = task.date,
                                        pointsEarned = task.points,
                                        completionTime = completionTime
                                    )

                                    historyItems.add(historyItem)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RepositoryDebug",
                                    "Error parsing task history item: ${e.message}"
                                )
                            }
                        }

                        val sortedItems = historyItems
                            .sortedWith(compareByDescending<TaskHistoryItem> { it.completionDate }
                                .thenByDescending { it.task.completedAt })
                            .take(limit)

                        Log.d(
                            "RepositoryDebug",
                            "Found ${sortedItems.size} questionnaire task history items"
                        )
                        continuation.resume(sortedItems)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting task history: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun getTaskHistoryStats(userId: String, month: String): TaskHistoryStats {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Getting task history stats for $month")

            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalCompleted = 0
                        var totalPoints = 0
                        var questionnaireTasksCompleted = 0
                        var dailyTasksCompleted = 0
                        val emotionCounts = mutableMapOf<String, Int>()
                        val completionDays = mutableSetOf<String>()

                        for (child in snapshot.children) {
                            try {
                                val task = child.getValue(DailyTask::class.java)
                                if (task != null && task.completed && task.date.startsWith(month)) {
                                    totalCompleted++
                                    totalPoints += task.points
                                    completionDays.add(task.date)

                                    when (task.category) {
                                        TaskCategory.QUESTIONNAIRE_BASED -> {
                                            questionnaireTasksCompleted++
                                            // Count emotions
                                            task.triggeringEmotionNames.forEach { emotion ->
                                                emotionCounts[emotion] =
                                                    (emotionCounts[emotion] ?: 0) + 1
                                            }
                                        }

                                        TaskCategory.DAILY_ROUTINE -> dailyTasksCompleted++
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RepositoryDebug",
                                    "Error parsing task for stats: ${e.message}"
                                )
                            }
                        }

                        val stats = TaskHistoryStats(
                            month = month,
                            totalTasksCompleted = totalCompleted,
                            totalPoints = totalPoints,
                            questionnaireTasksCompleted = questionnaireTasksCompleted,
                            dailyTasksCompleted = dailyTasksCompleted,
                            activeDays = completionDays.size,
                            topEmotions = emotionCounts.toList().sortedByDescending { it.second }
                                .take(5)
                        )

                        Log.d("RepositoryDebug", "Task history stats: $stats")
                        continuation.resume(stats)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "RepositoryDebug",
                            "Error getting task history stats: ${error.message}"
                        )
                        continuation.resume(TaskHistoryStats(month = month))
                    }
                })
        }
    }

    suspend fun getMonthlyProgress(userId: String, monthKey: String): Map<String, DailyProgress> {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Getting monthly progress for $userId, month: $monthKey")

            val result = mutableMapOf<String, DailyProgress>()

            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tasksByDate = mutableMapOf<String, MutableList<DailyTask>>()

                        for (child in snapshot.children) {
                            try {
                                val task = child.getValue(DailyTask::class.java)
                                if (task != null && task.date.startsWith(monthKey) && task.completed) {
                                    val dateKey = task.date
                                    tasksByDate.getOrPut(dateKey) { mutableListOf() }.add(task)
                                }
                            } catch (e: Exception) {
                                Log.e("RepositoryDebug", "Error parsing task: ${e.message}")
                            }
                        }

                        dailyQuestionnairesRef.orderByChild("userId").equalTo(userId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(questionnaireSnapshot: DataSnapshot) {
                                    val questionnairesByDate = mutableMapOf<String, Boolean>()

                                    for (child in questionnaireSnapshot.children) {
                                        try {
                                            val questionnaire =
                                                child.getValue(DailyQuestionnaire::class.java)
                                            if (questionnaire != null &&
                                                questionnaire.date.startsWith(monthKey) &&
                                                questionnaire.completed
                                            ) {
                                                questionnairesByDate[questionnaire.date] = true
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "RepositoryDebug",
                                                "Error parsing questionnaire: ${e.message}"
                                            )
                                        }
                                    }

                                    val allDates =
                                        (tasksByDate.keys + questionnairesByDate.keys).distinct()

                                    allDates.forEach { dateKey ->
                                        val dayTasks = tasksByDate[dateKey] ?: emptyList()
                                        val hasQuestionnaire =
                                            questionnairesByDate[dateKey] ?: false

                                        val dailyPoints = dayTasks.sumOf { it.points }
                                        val tasksCompleted = dayTasks.size
                                        val hasLogin = hasQuestionnaire || tasksCompleted > 0

                                        val dailyTasksCompleted = dayTasks.count {
                                            it.category == TaskCategory.DAILY_ROUTINE
                                        }
                                        val questionnaireTasksCompleted = dayTasks.count {
                                            it.category == TaskCategory.QUESTIONNAIRE_BASED
                                        }

                                        result[dateKey] = DailyProgress(
                                            date = dateKey,
                                            hasLogin = hasLogin,
                                            pointsEarned = dailyPoints,
                                            tasksCompleted = tasksCompleted,
                                            checkInCompleted = hasQuestionnaire,
                                            dailyTasksCompleted = dailyTasksCompleted,
                                            questionnaireTasksCompleted = questionnaireTasksCompleted
                                        )
                                    }

                                    Log.d(
                                        "RepositoryDebug",
                                        "Monthly progress loaded: ${result.size} active days in $monthKey"
                                    )
                                    continuation.resume(result)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(
                                        "RepositoryDebug",
                                        "Error getting questionnaires: ${error.message}"
                                    )
                                    continuation.resume(emptyMap())
                                }
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting tasks: ${error.message}")
                        continuation.resume(emptyMap())
                    }
                })
        }
    }

    suspend fun saveDiaryEntry(entry: DiaryEntry): Boolean {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Saving diary entry: ${entry.id}")

            val entryWithWordCount = entry.copy(
                wordCount = entry.calculateWordCount(),
                updatedAt = System.currentTimeMillis()
            )

            diaryEntriesRef.child(entry.id)
                .setValue(entryWithWordCount)
                .addOnSuccessListener {
                    Log.d("RepositoryDebug", "✅ Diary entry saved successfully: ${entry.id}")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    Log.e("RepositoryDebug", "❌ Failed to save diary entry: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    suspend fun getDiaryEntry(userId: String, date: String): DiaryEntry? {
        return suspendCancellableCoroutine { continuation ->
            val entryId = DiaryEntry.generateId(userId, date)

            diaryEntriesRef.child(entryId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val entry = snapshot.getValue(DiaryEntry::class.java)
                            continuation.resume(entry)
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting diary entry: ${error.message}")
                        continuation.resume(null)
                    }
                })
        }
    }

    suspend fun getDiaryEntries(
        userId: String,
        filter: DiaryFilter = DiaryFilter()
    ): List<DiaryEntry> {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Getting diary entries for user: $userId")

            diaryEntriesRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val entries = mutableListOf<DiaryEntry>()

                        for (child in snapshot.children) {
                            try {
                                val entry = child.getValue(DiaryEntry::class.java)
                                if (entry != null) {
                                    entries.add(entry.copy(id = child.key ?: ""))
                                }
                            } catch (e: Exception) {
                                Log.e("RepositoryDebug", "Error parsing diary entry: ${e.message}")
                            }
                        }

                        val filteredEntries = applyDiaryFilters(entries, filter)

                        Log.d("RepositoryDebug", "Found ${filteredEntries.size} diary entries")
                        continuation.resume(filteredEntries)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting diary entries: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    suspend fun deleteDiaryEntry(entryId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Deleting diary entry: $entryId")

            diaryEntriesRef.child(entryId)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("RepositoryDebug", "✅ Diary entry deleted successfully: $entryId")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    Log.e("RepositoryDebug", "❌ Failed to delete diary entry: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    suspend fun getDiaryStats(userId: String): DiaryStats {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Getting diary stats for user: $userId")

            diaryEntriesRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val entries = mutableListOf<DiaryEntry>()

                        for (child in snapshot.children) {
                            try {
                                val entry = child.getValue(DiaryEntry::class.java)
                                if (entry != null) {
                                    entries.add(entry)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RepositoryDebug",
                                    "Error parsing diary entry for stats: ${e.message}"
                                )
                            }
                        }

                        val stats = calculateDiaryStats(entries)
                        Log.d(
                            "RepositoryDebug",
                            "Diary stats calculated: ${stats.totalEntries} entries"
                        )
                        continuation.resume(stats)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting diary stats: ${error.message}")
                        continuation.resume(DiaryStats())
                    }
                })
        }
    }

    suspend fun hasDiaryEntryToday(userId: String): Boolean {
        val today = DiaryEntry.getCurrentDateString()
        val entry = getDiaryEntry(userId, today)
        return entry != null
    }


    private fun applyDiaryFilters(
        entries: List<DiaryEntry>,
        filter: DiaryFilter
    ): List<DiaryEntry> {
        var filtered = entries


        if (filter.startDate.isNotEmpty()) {
            filtered = filtered.filter { it.date >= filter.startDate }
        }
        if (filter.endDate.isNotEmpty()) {
            filtered = filtered.filter { it.date <= filter.endDate }
        }


        if (filter.mood.isNotEmpty()) {
            filtered = filtered.filter { it.mood == filter.mood }
        }


        if (filter.searchText.isNotEmpty()) {
            val searchLower = filter.searchText.lowercase()
            filtered = filtered.filter { entry ->
                entry.title.lowercase().contains(searchLower) ||
                        entry.content.lowercase().contains(searchLower)
            }
        }

        if (filter.tags.isNotEmpty()) {
            filtered = filtered.filter { entry ->
                filter.tags.any { tag -> entry.tags.contains(tag) }
            }
        }

        return when (filter.sortBy) {
            DiarySortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            DiarySortOption.DATE_ASC -> filtered.sortedBy { it.date }
            DiarySortOption.WORD_COUNT_DESC -> filtered.sortedByDescending { it.wordCount }
            DiarySortOption.WORD_COUNT_ASC -> filtered.sortedBy { it.wordCount }
            DiarySortOption.TITLE_ASC -> filtered.sortedBy { it.title }
        }
    }

    private fun calculateDiaryStats(entries: List<DiaryEntry>): DiaryStats {
        if (entries.isEmpty()) {
            return DiaryStats()
        }

        val totalEntries = entries.size
        val totalWords = entries.sumOf { it.wordCount }
        val averageWordsPerEntry = if (totalEntries > 0) totalWords / totalEntries else 0
        val longestEntry = entries.maxOfOrNull { it.wordCount } ?: 0

        val sortedByDate = entries.sortedByDescending { it.date }
        var currentStreak = 0
        val today = DiaryEntry.getCurrentDateString()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            val todayDate = dateFormat.parse(today)
            if (todayDate != null) {
                var checkDate = todayDate
                val calendar = Calendar.getInstance()

                for (i in 0 until 365) {
                    val checkDateStr = dateFormat.format(checkDate)
                    val hasEntry = sortedByDate.any { it.date == checkDateStr }

                    if (hasEntry) {
                        currentStreak++
                    } else {
                        break
                    }

                    calendar.time = checkDate
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    checkDate = calendar.time
                }
            }
        } catch (e: Exception) {
            Log.e("RepositoryDebug", "Error calculating streak: ${e.message}")
        }

        val activeDays = entries.map { it.date }.distinct().size

        val moodCounts = entries.groupingBy { it.mood }.eachCount()
        val mostCommonMood = moodCounts.maxByOrNull { it.value }

        val allTags = entries.flatMap { it.tags }
        val tagCounts = allTags.groupingBy { it }.eachCount()
        val topTags = tagCounts.toList().sortedByDescending { it.second }.take(5)

        val entriesByMonth = entries.groupingBy { entry ->
            entry.date.substring(0, 7)
        }.eachCount()

        return DiaryStats(
            totalEntries = totalEntries,
            totalWords = totalWords,
            averageWordsPerEntry = averageWordsPerEntry,
            longestEntry = longestEntry,
            currentStreak = currentStreak,
            activeDays = activeDays,
            mostCommonMood = mostCommonMood?.key ?: "",
            mostCommonMoodCount = mostCommonMood?.value ?: 0,
            topTags = topTags,
            entriesByMonth = entriesByMonth
        )
    }

    suspend fun getTodaysMentalHealthTip(userId: String): MentalHealthTip? {
        return suspendCancellableCoroutine { continuation ->
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val tipId = MentalHealthTip.generateId(userId, today)

            Log.d("RepositoryDebug", "Looking for tip with ID: $tipId")

            mentalHealthTipsRef.child(tipId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            Log.d("RepositoryDebug", "Raw Firebase data: ${snapshot.value}")

                            val tip = snapshot.getValue(MentalHealthTip::class.java)

                            if (tip != null) {
                                tip.syncFields()

                                Log.d(
                                    "RepositoryDebug",
                                    "Parsed tip - ID: ${tip.id}, isViewed: ${tip.isViewed}, pointsAwarded: ${tip.pointsAwarded}"
                                )
                            }

                            continuation.resume(tip)
                        } else {
                            Log.d("RepositoryDebug", "No tip found for today")
                            continuation.resume(null)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting today's tip: ${error.message}")
                        continuation.resume(null)
                    }
                })
        }
    }

    suspend fun generateTodaysMentalHealthTip(userId: String): MentalHealthTip? {
        return try {
            Log.d("RepositoryDebug", "Generating new mental health tip for user: $userId")

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val recentEmotions = getRecentUserEmotions(userId, 7)
            val recentTasks = getRecentCompletedTasks(userId, 7)
            val previousTips = getRecentTipTitles(userId, 7)

            val request = TipGenerationRequest(
                userId = userId,
                date = today,
                userEmotions = recentEmotions,
                recentTasks = recentTasks,
                previousTips = previousTips,
                userPreferences = TipPreferences()
            )

            val generatedTip = openAIService.generateMentalHealthTip(request)

            if (generatedTip != null) {
                generatedTip.syncFields()
                val saved = saveMentalHealthTip(generatedTip)
                if (saved) {
                    Log.d("RepositoryDebug", "✅ Generated and saved tip: ${generatedTip.title}")
                    generatedTip
                } else {
                    Log.e("RepositoryDebug", "❌ Failed to save generated tip")
                    null
                }
            } else {
                Log.e("RepositoryDebug", "❌ Failed to generate tip")
                null
            }

        } catch (e: Exception) {
            Log.e("RepositoryDebug", "Error generating tip: ${e.message}", e)
            null
        }
    }

    suspend fun saveMentalHealthTip(tip: MentalHealthTip): Boolean {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Saving mental health tip: ${tip.id}")

            tip.syncFields()

            mentalHealthTipsRef.child(tip.id)
                .setValue(tip)
                .addOnSuccessListener {
                    Log.d("RepositoryDebug", "✅ Mental health tip saved successfully: ${tip.id}")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    Log.e("RepositoryDebug", "❌ Failed to save mental health tip: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    suspend fun markTipAsViewed(tipId: String): Boolean {
        return suspendCancellableCoroutine { continuation ->
            Log.d("RepositoryDebug", "Marking tip as viewed: $tipId")

            mentalHealthTipsRef.child(tipId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val currentTip = snapshot.getValue(MentalHealthTip::class.java)

                            if (currentTip?.isViewed == true) {
                                Log.d("RepositoryDebug", "Tip already marked as viewed: $tipId")
                                continuation.resume(true)
                                return
                            }

                            val currentTime = System.currentTimeMillis()
                            val updates = mutableMapOf<String, Any>(
                                "mental_health_tips/$tipId/isViewed" to true,
                                "mental_health_tips/$tipId/viewed" to true,
                                "mental_health_tips/$tipId/viewedAt" to currentTime,
                                "mental_health_tips/$tipId/pointsAwarded" to true
                            )

                            database.reference.updateChildren(updates)
                                .addOnSuccessListener {
                                    Log.d("RepositoryDebug", "✅ Tip marked as viewed: $tipId")
                                    continuation.resume(true)
                                }
                                .addOnFailureListener { error ->
                                    Log.e(
                                        "RepositoryDebug",
                                        "❌ Failed to mark tip as viewed: ${error.message}"
                                    )
                                    continuation.resume(false)
                                }
                        } else {
                            Log.e("RepositoryDebug", "Tip not found: $tipId")
                            continuation.resume(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error checking tip status: ${error.message}")
                        continuation.resume(false)
                    }
                })
        }
    }

    private suspend fun getRecentUserEmotions(userId: String, days: Int): List<String> {
        return suspendCancellableCoroutine { continuation ->
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -days)
            }.time
            val cutoffDateStr =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cutoffDate)

            dailyQuestionnairesRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val emotions = mutableSetOf<String>()

                        for (child in snapshot.children) {
                            try {
                                val questionnaire = child.getValue(DailyQuestionnaire::class.java)
                                if (questionnaire != null &&
                                    questionnaire.date >= cutoffDateStr &&
                                    questionnaire.completed
                                ) {
                                    emotions.addAll(questionnaire.selectedEmotions)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RepositoryDebug",
                                    "Error parsing questionnaire for emotions: ${e.message}"
                                )
                            }
                        }

                        continuation.resume(emotions.toList())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting recent emotions: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }

    private suspend fun getRecentCompletedTasks(userId: String, days: Int): List<String> {
        return suspendCancellableCoroutine { continuation ->
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -days)
            }.time
            val cutoffDateStr =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cutoffDate)

            dailyTasksRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val taskTitles = mutableSetOf<String>()

                        for (child in snapshot.children) {
                            try {
                                val task = child.getValue(DailyTask::class.java)
                                if (task != null &&
                                    task.date >= cutoffDateStr &&
                                    task.completed
                                ) {
                                    taskTitles.add(task.title)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RepositoryDebug",
                                    "Error parsing task for titles: ${e.message}"
                                )
                            }
                        }

                        continuation.resume(taskTitles.take(10).toList())
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("RepositoryDebug", "Error getting recent tasks: ${error.message}")
                        continuation.resume(emptyList())
                    }
                })
        }
    }


    private suspend fun getRecentTipTitles(userId: String, days: Int): List<String> {
        return suspendCancellableCoroutine { continuation ->
            val cutoffDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -days)
            }.time
            val cutoffDateStr =
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cutoffDate)

            mentalHealthTipsRef.orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tipTitles = mutableListOf<String>()

                        for (child in snapshot.children) {
                            try {
                                val tip = child.getValue(MentalHealthTip::class.java)
                                if (tip != null && tip.date >= cutoffDateStr) {
                                    tipTitles.add(tip.title)
                                }
                            } catch (e: Exception) {
                                Log.e(
                                    "RepositoryDebug",
                                    "Error parsing tip for titles: ${e.message}"
                                )
                            }
                        }

                        continuation.resume(tipTitles)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(
                            "RepositoryDebug",
                            "Error getting recent tip titles: ${error.message}"
                        )
                        continuation.resume(emptyList())
                    }
                })
        }
    }
}