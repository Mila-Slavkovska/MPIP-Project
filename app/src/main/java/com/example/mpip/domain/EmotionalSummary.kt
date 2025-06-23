package com.example.mpip.domain

data class EmotionalSummary(
    val userId: String = "",
    val weekStart: String = "",
    val emotionFrequency: Map<String, Int> = emptyMap(),
    val averageMoodScore: Float = 0f,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val totalPoints: Int = 0
)
