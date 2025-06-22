package com.example.mpip.domain

import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.domain.enums.tasks.TaskDifficulty
import com.example.mpip.domain.enums.tasks.TaskType

data class DailyTask(
    val id: String = "",
    val userId: String = "",
    val templateId: String = "",
    val title: String = "",
    val description: String = "",
    val type: TaskType = TaskType.SIMPLE_ACTION,
    val category: TaskCategory = TaskCategory.DAILY_ROUTINE,
    val points: Int = 10,
    val date: String = "",
    val completed: Boolean = false,
    val completedAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val associatedEmotions: List<String> = emptyList(),
    val difficulty: TaskDifficulty = TaskDifficulty.EASY,
    val userResponse: String = "",
    val photoPath: String = "",
    val questionnaireId: String = "",
    val triggeringEmotionNames: List<String> = emptyList(),
    val questionnaireMemo: String = "",
    val questionnaireRelations: List<String> = emptyList()
)
