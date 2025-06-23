package com.example.mpip.domain

import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.domain.enums.tasks.TaskDifficulty
import com.example.mpip.domain.enums.tasks.TaskType

data class TaskTemplate(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: TaskType = TaskType.SIMPLE_ACTION,
    val category: TaskCategory = TaskCategory.DAILY_ROUTINE,
    val points: Int = 10,
    val isActive: Boolean = true,
    val triggerEmotions: List<String> = emptyList(),
    val difficulty: TaskDifficulty = TaskDifficulty.EASY
)
