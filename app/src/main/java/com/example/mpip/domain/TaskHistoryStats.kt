package com.example.mpip.domain

data class TaskHistoryStats(
    val month: String,
    val totalTasksCompleted: Int = 0,
    val totalPoints: Int = 0,
    val questionnaireTasksCompleted: Int = 0,
    val dailyTasksCompleted: Int = 0,
    val activeDays: Int = 0,
    val topEmotions: List<Pair<String, Int>> = emptyList()
)
