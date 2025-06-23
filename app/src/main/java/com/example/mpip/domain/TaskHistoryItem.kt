package com.example.mpip.domain

data class TaskHistoryItem(
    val task: DailyTask,
    val questionnaire: DailyQuestionnaire?,
    val completionDate: String,
    val pointsEarned: Int,
    val completionTime: String
)
