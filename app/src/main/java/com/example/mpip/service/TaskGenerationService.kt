package com.example.mpip.service

import android.util.Log
import com.example.mpip.domain.DailyQuestionnaire
import com.example.mpip.domain.DailyTask
import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.repository.Repository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskGenerationService(private val repository: Repository) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    suspend fun generateDailyRoutineTasks(userId: String): List<DailyTask> {
        val today = dateFormat.format(Date())

        Log.d("TaskGeneration", "=== GENERATING DAILY ROUTINE TASKS FOR USER: $userId ===")

        val routineTemplates = repository.getActiveDailyRoutineTasks()

        Log.d("TaskGeneration", "Found ${routineTemplates.size} daily routine templates")

        val dailyTasks = routineTemplates.mapIndexed { index, template ->
            val uniqueId = "${userId}_daily_${template.id}_${today}"

            DailyTask(
                id = uniqueId,
                userId = userId,
                templateId = template.id,
                title = template.title,
                description = template.description,
                type = template.type,
                category = TaskCategory.DAILY_ROUTINE,
                points = template.points,
                date = today,
                completed = false,
                createdAt = System.currentTimeMillis(),
                associatedEmotions = emptyList(),
                difficulty = template.difficulty,
                questionnaireId = ""
            )
        }

        Log.d("TaskGeneration", "Generated ${dailyTasks.size} daily routine tasks")
        return dailyTasks
    }

    suspend fun generateQuestionnaireBasedTasks(
        userId: String,
        questionnaire: DailyQuestionnaire
    ): List<DailyTask> {
        val today = dateFormat.format(Date())
        val selectedEmotionIds = questionnaire.selectedEmotions

        Log.d("TaskGeneration", "=== GENERATING QUESTIONNAIRE TASKS FOR USER: $userId ===")
        Log.d("TaskGeneration", "Selected emotions: $selectedEmotionIds")

        val questionnaireTemplates =
            repository.getLimitedTaskTemplatesForEmotions(selectedEmotionIds, 3)

        Log.d("TaskGeneration", "Found ${questionnaireTemplates.size} questionnaire templates")

        val questionnaireTasks = questionnaireTemplates.mapIndexed { index, template ->
            val uniqueId = "${userId}_questionnaire_${template.id}_${today}_$index"

            DailyTask(
                id = uniqueId,
                userId = userId,
                templateId = template.id,
                title = template.title,
                description = template.description,
                type = template.type,
                category = TaskCategory.QUESTIONNAIRE_BASED,
                points = template.points,
                date = today,
                completed = false,
                createdAt = System.currentTimeMillis(),
                associatedEmotions = selectedEmotionIds.filter { emotionId ->
                    template.triggerEmotions.isEmpty() || template.triggerEmotions.contains(
                        emotionId
                    )
                },
                difficulty = template.difficulty,
                questionnaireId = "${questionnaire.userId}_${questionnaire.date}",
                // ADD QUESTIONNAIRE CONTEXT HERE
                triggeringEmotionNames = questionnaire.selectedEmotionNames,
                questionnaireMemo = questionnaire.memo,
                questionnaireRelations = questionnaire.selectedRelationNames,
                userResponse = "",
                photoPath = ""
            )
        }

        Log.d(
            "TaskGeneration",
            "Generated ${questionnaireTasks.size} questionnaire-based tasks with context"
        )
        questionnaireTasks.forEach { task ->
            Log.d(
                "TaskGeneration",
                "Task: ${task.title} | Emotions: ${task.triggeringEmotionNames}"
            )
        }

        return questionnaireTasks
    }

    suspend fun generateDailyTasks(
        userId: String,
        questionnaire: DailyQuestionnaire
    ): List<DailyTask> {
        return generateQuestionnaireBasedTasks(userId, questionnaire)
    }
}