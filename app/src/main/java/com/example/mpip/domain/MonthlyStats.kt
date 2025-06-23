package com.example.mpip.domain

data class MonthlyStats(
    val month: String = "",
    val totalPoints: Int = 0,
    val tasksCompleted: Int = 0,
    val loginDays: List<String> = emptyList(),
    val streakDays: Int = 0,
    val pointsSpent: Int = 0
)
